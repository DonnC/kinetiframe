"""Four authored 2D views, built from reusable source artwork and the shipping skin binder."""
from PIL import Image
from street_actor import ROOT, actor

VIEWS = ('FRONT', 'PROFILE', 'THREE_QUARTER', 'BACK')


def cast_actor(character, view, directional=False, gestures=False, joined=False):
    gestures = gestures or joined
    directional = directional or gestures
    revision, artwork = actor(character, view == 'PROFILE')
    # Keep the character identity; repaired views are new immutable snapshots. Old gestures
    # retain their original revision IDs instead of silently changing their appearance.
    aid = f'ls-{character}-all-views-v{3 if gestures else 2 if directional else 1}'
    rid = f'{aid}-{view.lower()}' + ('-joined-v4' if joined else '')
    graph = revision['sceneGraph']
    bones = graph['bones']
    source = ROOT / f'docs/art-direction/loose-signal/characters/{character}/full-body/v4/cutouts'
    if view in ('FRONT', 'BACK') or character == 'mira' and view == 'PROFILE':
        for part, height, pivot in [('head', 145, (.5, .96)), ('torso', 120, (.5, .92))]:
            if character == 'mira' and view == 'PROFILE' and part == 'torso':
                continue  # The existing socket-free torso is cleaner than the new atlas opening.
            image = Image.open(source / f'{view.lower()}-{part}.png').convert('RGBA')
            artwork[part] = image
            bones[part]['textureBinding'].update(pivotX=image.width * pivot[0], pivotY=image.height * pivot[1],
                                               scale=height / image.height)
    if view in ('FRONT', 'BACK'):
        for side, sign in [('left', -1), ('right', 1)]:
            bones[f'{side}_upper_arm']['localTransform']['y'] = sign * 35
            bones[f'{side}_thigh']['localTransform']['y'] = sign * 16
        for part in ('eyes', 'mouth'):
            bones[part]['localTransform']['y'] = 0
        if view == 'BACK':
            for part in ('eyes', 'mouth'):
                del bones[part]
                artwork = {address: image for address, image in artwork.items() if address.split('#')[0] != part}
    if directional:
        from directional_bindings import attach_directional_artwork
        attach_directional_artwork(character, view, graph, artwork, joined=joined)
        if gestures:
            from directional_bindings import attach_directional_hands
            attach_directional_hands(character, view, graph, artwork)
    # Face anchor and root share scale across views; switching views is an authored cut,
    # never an invented rotation of a flat illustration.
    revision.update(actorId=aid, revisionId=rid, view=view,
                    artworkAssetPaths={a: f'artwork/{rid}/{a.replace("#", "--")}.png' for a in artwork})
    return revision, artwork
