"""Offline skin bind data; native tests compare every vertex against ArtworkSkinBuilder.

No artwork is generated/repainted here. Sources remain original transparent raster pixels.
The review movie is rendered by Android, not by a second approximation of skinning.
"""
import math

def bind_attachment(graph, owner, binding, image):
    """Same proximal-fifth socket blend as shipping ArtworkAttachmentBlend, not painted filler."""
    from street_actor import world_pose
    pose = world_pose(graph, {bid: bone['localTransform']['rotationDegrees'] for bid, bone in graph['bones'].items()}, {})
    def matrix(bid):
        (x, y), angle = pose[bid]; r = math.radians(angle)
        return dict(a=math.cos(r), b=-math.sin(r), c=math.sin(r), d=math.cos(r), tx=x, ty=y)
    world = matrix(owner); parent = graph['bones'][owner]['parentId']
    r = math.radians(binding.get('rotationDegrees', 0)); scale = binding['scale']
    columns, rows = max(4, min(32, image.width//12)), max(4, min(32, image.height//12))
    vertices = []
    for row in range(rows+1):
        for column in range(columns+1):
            sx = image.width*column/columns-binding['pivotX']; sy = image.height*row/rows-binding['pivotY']
            lx = (sx*math.cos(r)-sy*math.sin(r))*scale+binding.get('offsetX', 0)
            ly = (sx*math.sin(r)+sy*math.cos(r))*scale+binding.get('offsetY', 0)
            t = max(0, min(1, lx/(graph['bones'][owner]['length']*.2)))
            w = t*t*(3-2*t)
            vertices.append(dict(x=world['tx']+world['a']*lx+world['b']*ly,
                                 y=world['ty']+world['c']*lx+world['d']*ly, weights=[1-w, w]))
    return dict(sourceWidth=image.width, sourceHeight=image.height, columns=columns, rows=rows,
                bones=[dict(boneId=parent, bindWorld=matrix(parent)), dict(boneId=owner, bindWorld=world)],
                vertices=vertices, attachmentBlend=True)


def bind_skin(graph, owner, child, binding, image, softness=.25):
    from street_actor import world_pose
    pose = world_pose(graph, {bid: bone['localTransform']['rotationDegrees'] for bid, bone in graph['bones'].items()}, {})
    def affine(bid):
        (x, y), angle = pose[bid]
        r = math.radians(angle)
        return dict(a=math.cos(r), b=-math.sin(r), c=math.sin(r), d=math.cos(r), tx=x, ty=y)
    upper, lower = affine(owner), affine(child)
    angle = math.radians(pose[owner][1] + binding.get('rotationDegrees', 0))
    scale = binding['scale']
    a, b, c, d = math.cos(angle)*scale, -math.sin(angle)*scale, math.sin(angle)*scale, math.cos(angle)*scale
    columns, rows = max(4, min(32, image.width//12)), max(4, min(32, image.height//12))
    radius = min(graph['bones'][owner]['length'], graph['bones'][child]['length'])*softness
    def distance(x, y, matrix, length):
        vx, vy = matrix['a']*length, matrix['c']*length
        t = max(0, min(1, ((x-matrix['tx'])*vx+(y-matrix['ty'])*vy)/(vx*vx+vy*vy)))
        return math.hypot(x-matrix['tx']-t*vx, y-matrix['ty']-t*vy)
    vertices = []
    for row in range(rows+1):
        for column in range(columns+1):
            u, v = image.width*column/columns-binding['pivotX'], image.height*row/rows-binding['pivotY']
            x, y = upper['tx']+a*u+b*v, upper['ty']+c*u+d*v
            du = distance(x, y, upper, graph['bones'][owner]['length'])
            dl = distance(x, y, lower, graph['bones'][child]['length'])
            t = max(0, min(1, .5+(du-dl)/(2*radius)))
            weight = t*t*(3-2*t)
            vertices.append(dict(x=x, y=y, weights=[1-weight, weight]))
    return dict(sourceWidth=image.width, sourceHeight=image.height, columns=columns, rows=rows,
        bones=[dict(boneId=owner, bindWorld=upper), dict(boneId=child, bindWorld=lower)], vertices=vertices)


def attach_continuous_limbs(graph, artwork, sources):
    for side in ['left', 'right']:
        for upper, lower, source, height, pivot in [
            (side+'_upper_arm', side+'_forearm', 'sleeve', 122.2, (.5, .05)),
            (side+'_thigh', side+'_shin', 'trouser-leg', 190, (.5, .035))]:
            image = sources[source]
            binding = dict(assetId=upper, pivotX=image.width*pivot[0], pivotY=image.height*pivot[1],
                scale=height/image.height, rotationDegrees=-90)
            binding['skin'] = bind_skin(graph, upper, lower, binding, image)
            graph['bones'][upper]['textureBinding'] = binding
            artwork[upper] = image
            # Shoulder caps sit under the uncut torso, hands remain independently in front.
            if source == 'sleeve': graph['bones'][upper]['drawOrder'] = 15 if side == 'left' else 18
