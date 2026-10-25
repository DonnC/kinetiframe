# Dependency wiring

Accepted by the maintainer on 25 October 2026.

## Context

Screens build their own storage today. `AppDatabase.getInstance` is called in seven places, five
of them inside composables, and the app constructs a repository or store in 39 places, usually
with `remember` in a route. `EditorRepositories` builds the editor's stores inside a composable.
This works, but it ties screens to storage, which the [UI in presentation](ui-in-presentation.md)
decision rules out: `presentation` must not import `data`, so a screen can't create the
repository it needs.

The object graph is small. One Room database, one document store under `actor-studio/`, about
ten repositories, the preference store and the crash report store, used by one Activity and 11
ViewModels. There are no background workers and no flows with their own lifetime.

We considered Hilt. It would remove the hand-written ViewModel factories and give scopes and
test bindings, but it needs code generation in every module that declares injected classes or
bindings: `app`, `presentation`, `studio`, `actors` and `data`. A full check already takes 15 to
28 minutes on a laptop, and code generation would add to every incremental build, for a graph of
about fifteen objects. Hilt also needs extra setup for ViewModels scoped to Navigation 3
back-stack entries.

## Decision

### One container in `app`

`AppContainer`, in `app`, creates each shared object once, lazily, and holds it for the life of
the process. `KinetiFrameApplication` owns the container. Nothing else calls
`AppDatabase.getInstance` or constructs a repository or store.

```kotlin
class AppContainer(context: Context) {
    val files by lazy { DocumentFileStore(File(context.filesDir, "actor-studio")) }
    val database by lazy { AppDatabase.getInstance(context) }
    val actors by lazy { ActorRepository(files, dao = database.actorDao()) }
    // …one property per shared object
}
```

### One ViewModel factory

`AppViewModelFactory`, in `app`, builds every ViewModel from the container. `MainActivity`
provides it to the whole UI through `LocalViewModelFactory`, which `presentation` defines, and a
screen asks for its ViewModel with:

```kotlin
val model: LibraryViewModel = viewModel(factory = LocalViewModelFactory.current)
```

Arguments such as a project id travel as `CreationExtras` keys, declared next to the ViewModel
that reads them, and the factory reads them when it builds the ViewModel. ViewModels still get
everything through their constructors, so tests build them directly with fakes or temporary
stores.

This is the same idea as Hilt's `hiltViewModel()`, without the code generation. Screens never
take a repository, a store or a `Context` to build one, and a component deep in the tree, such as
the palette in every colour popup, gets its ViewModel without its parents passing anything down.
ViewModels stay scoped to their navigation entry, as they are today.

### Sharing is the default

Repositories are shared across screens. They hold no screen state, and the one cache that exists,
`LibraryRepository`'s pack commits, stays consistent when every screen writes through the same
instance. A screen that needs its own instance gets a factory function on the container instead
of a property.

### When to revisit

Move to Hilt if any of these become true: KinetiFrame gains a second Activity, WorkManager jobs
such as background export or cloud backup, or object graphs scoped to a flow. Each container
property then becomes one Hilt binding, so the change is mechanical.

## Consequences

- Every dependency is visible in plain Kotlin in one file, and costs nothing at build time.
- Tests build ViewModels directly with fakes or temporary stores, as they do today.
- Each ViewModel needs one `initializer` block in `AppViewModelFactory`, written by hand.
- Screens move into `presentation` once they take a ViewModel instead of building storage.
