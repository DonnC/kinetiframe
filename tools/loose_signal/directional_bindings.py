"""Authored view sockets; only shipping TextureBinding/skin fields, no special renderer."""
from PIL import Image
from street_actor import ROOT
from street_skin import bind_skin


def alpha_center_at(image, fraction):
    """Measure the authored neck stem without repainting source pixels."""
    y = min(image.height-1, round(image.height*fraction))
    occupied = [x for x in range(image.width) if image.getpixel((x, y))[3] > 150]
    if not occupied: raise ValueError(f'No neck artwork at {fraction}')
    return (occupied[0]+occupied[-1])/2


def attach_directional_hands(character, view, graph, artwork):
    """Register every swap at its authored wrist, not the sprite's bounding centre.

    Expressive hands point up in source space; the existing 180-degree binding
    correction keeps their wrist orientation consistent with relaxed hands.
    """
    source = ROOT / f'docs/art-direction/loose-signal/characters/{character}/full-body/v6/cutouts'
    for side in ('left', 'right'):
        bone = graph['bones'][f'{side}_hand']
        rest_image = artwork[f'{side}_hand']
        rest_scale = bone['textureBinding']['scale']
        def wrist_width(image, fraction):
            y = min(image.height-1, round(image.height*fraction))
            xs = [x for x in range(image.width) if image.getpixel((x,y))[3] > 150]
            if not xs: raise ValueError('Missing hand wrist')
            return xs[-1]-xs[0]+1
        # Match wrist diameter across artwork swaps so palms don't grow a thick
        # wrist or leave a cuff gap merely because their fingers are taller.
        diameter = wrist_width(rest_image, .05)*rest_scale
        for variant in ('palm', 'point', 'fist'):
            suffix = '-left' if side == 'left' and view != 'PROFILE' else ''
            image = Image.open(source / f'{view.lower()}-hand-{variant}{suffix}.png').convert('RGBA')
            address = f'{side}_hand#{variant}'
            artwork[address] = image
            bone['artworkVariants'][variant]['binding'].update(
                pivotX=alpha_center_at(image, .95), pivotY=image.height*.95,
                scale=diameter/wrist_width(image, .95), rotationDegrees=180)


def attach_directional_artwork(character, view, graph, artwork, joined=False):
    source = ROOT / f'docs/art-direction/loose-signal/characters/{character}/full-body/v5/cutouts'
    bones = graph['bones']
    for part, height, pivot in [('head', 145, (.5, .90)), ('torso', 120, (.5, .92))]:
        body_source = ROOT / f'docs/art-direction/loose-signal/characters/{character}/full-body/v7/cutouts' if joined else source
        image = Image.open(body_source / f'{view.lower()}-{part}.png').convert('RGBA')
        artwork[part] = image
        pivot_x = alpha_center_at(image, .96) if part == 'head' else image.width*pivot[0]
        bones[part]['textureBinding'].update(pivotX=pivot_x, pivotY=image.height*pivot[1],
                                            scale=height/image.height)
    if joined:
        # Shirt/chest covers the open end of the head's neck tab. The head remains above
        # sleeves, but below the torso: no double horizontal cap over exposed skin.
        bones['head']['drawOrder'] = bones['torso']['drawOrder'] - 1
    torso_image = artwork['torso']
    torso_binding = bones['torso']['textureBinding']
    bones['head']['localTransform']['y'] = (alpha_center_at(torso_image, .03)-torso_binding['pivotX'])*torso_binding['scale']
    if view != 'BACK':
        head_image = artwork['head']
        binding = bones['head']['textureBinding']
        face_x = .50 if view == 'FRONT' else .84 if view == 'PROFILE' else .72
        for part, face_y in [('eyes', .40), ('mouth', .62)]:
            anchor_x = .78 if part == 'mouth' and view == 'PROFILE' else face_x
            bones[part]['localTransform'].update(
                x=(binding['pivotY']-head_image.height*face_y)*binding['scale'],
                y=(head_image.width*anchor_x-binding['pivotX'])*binding['scale'])
        if view == 'PROFILE':
            # A front-facing smile is wider than the visible cheek in a side view.
            # Keep the shared mouth artwork, but author a smaller profile placement.
            bones['mouth']['artworkVariants']['open']['binding']['scale'] *= .75
    # Rear limbs remain rigged. Identical rest sockets hide them by occlusion,
    # while independently posing them reveals the far-side arm/leg in a walk.
    if view == 'PROFILE':
        for side in ('left', 'right'):
            bones[f'{side}_upper_arm']['localTransform']['y'] = 0
            bones[f'{side}_thigh']['localTransform']['y'] = 0
        bones['left_hand']['drawOrder'] = 16
    for side in ('left', 'right'):
        for part, height, pivot in [('foot', 24, (.5 if view in ('FRONT', 'BACK') else .25, .12)),
                                    ('hand', 35, (.5, .10))]:
            file_part = 'hand-rest' if part == 'hand' else part
            name = f'{view.lower()}-{file_part}'
            if part == 'hand' and side == 'left' and view != 'PROFILE': name += '-left'
            image = Image.open(source / f'{name}.png').convert('RGBA')
            address = f'{side}_{part}'
            artwork[address] = image
            bones[address]['textureBinding'].update(pivotX=image.width*pivot[0], pivotY=image.height*pivot[1],
                                                   scale=height/image.height)
            if joined:
                # Register the wrist at the end of its insertion tab, not at a percentage
                # of the finger silhouette. Shoes tuck behind trouser hems at the ankle.
                depth = .22 if part == 'hand' else .28
                bones[address]['textureBinding'].update(pivotX=alpha_center_at(image, depth), pivotY=image.height*depth)
                if part == 'foot': bones[address]['drawOrder'] = bones[f'{side}_thigh']['drawOrder'] - 1
        # Changed rest sockets require fresh bind matrices or meshes snap back to
        # their old three-quarter bind pose when the same performance is applied.
        for owner, child in [(f'{side}_upper_arm', f'{side}_forearm'), (f'{side}_thigh', f'{side}_shin')]:
            binding = bones[owner]['textureBinding']
            binding['skin'] = bind_skin(graph, owner, child, binding, artwork[owner])
    if joined:
        from street_skin import bind_attachment
        head_binding = bones['head']['textureBinding']
        head_binding['skin'] = bind_attachment(graph, 'head', head_binding, artwork['head'])
