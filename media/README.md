# media

Sound, video and image handling that the studio, the library and export share. It depends on
`core` only and holds no screens: the recorder, trimmer and preview controls live in
`presentation.media.audio`. It never reads studio or actor state: the studio's playback managers
live in `app` under `studio/media` and call into this module.

| Package | Contents |
| --- | --- |
| `audio` | Decoding to PCM and mixing (`AudioMixer`, `StreamingAudioMix`), envelopes and crossfades, trimming, waveforms, voice filters and the recorder |
| `audio.data` | `VoiceEffectProxy`, which writes a voice effect to a WAV file that preview and export both read |
| `video` | Imported video metadata, frame decoding, framing and scene geometry, and keeping a linked video and its audio on one frame clock |
| `image` | Background and frame image decoding with memory-aware sample sizes |

Callers run decoding and mixing off the main thread.

```sh
./gradlew :media:testDebugUnitTest
```

Licensed under [Apache-2.0](../LICENSE).
