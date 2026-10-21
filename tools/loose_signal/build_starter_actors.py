"""Build the current reusable actor catalog without project cels or performances."""
import io
import json
import zipfile

from cast_views import VIEWS, cast_actor
from street_actor import ROOT, STAMP

OUTPUT = ROOT / 'app/src/main/assets/actor/starter-actors-v4.zip'


def encoded(value):
    return json.dumps(value, sort_keys=True, separators=(',', ':')).encode()


def png(image):
    buffer = io.BytesIO()
    image.save(buffer, format='PNG')
    return buffer.getvalue()


def entry(archive, name, data):
    info = zipfile.ZipInfo(name, (2026, 10, 10, 0, 0, 0))
    info.compress_type = zipfile.ZIP_DEFLATED
    archive.writestr(info, data)


def build():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    temporary = OUTPUT.with_suffix('.building')
    with zipfile.ZipFile(temporary, 'w') as archive:
        for character in ('tavi', 'mira', 'kuda'):
            views = [cast_actor(character, view, directional=True, gestures=True, joined=True)
                     for view in VIEWS]
            first = views[0][0]
            manifest = dict(schemaVersion=1, actorId=first['actorId'],
                            displayName=f'{character.title()} · All four views', templateId='custom-v1',
                            currentRevisionId=first['revisionId'], createdAtEpochMs=STAMP,
                            updatedAtEpochMs=STAMP,
                            viewRevisions={revision['view']: revision['revisionId'] for revision, _ in views})
            prefix = f'dependencies/actors/{first["actorId"]}/'
            entry(archive, prefix + 'manifest.json', encoded(manifest))
            for revision, images in views:
                entry(archive, prefix + 'revisions/' + revision['revisionId'] + '.json', encoded(revision))
                for address, image in images.items():
                    entry(archive, prefix + revision['artworkAssetPaths'][address], png(image))
    temporary.replace(OUTPUT)
    print(OUTPUT)


if __name__ == '__main__':
    build()
