"""Retarget local pose estimates to the existing 5+5 arm / 6+6 leg model units."""
import argparse,json,math
from pathlib import Path
import numpy as np

def unit(v):return v/max(np.linalg.norm(v),1e-8)
def quat(matrix):
    # Symmetric eigenvalue formulation, returned as x,y,z,w.
    m=matrix;K=np.array([[m[0,0]-m[1,1]-m[2,2],m[1,0]+m[0,1],m[2,0]+m[0,2],m[2,1]-m[1,2]],
      [m[1,0]+m[0,1],m[1,1]-m[0,0]-m[2,2],m[2,1]+m[1,2],m[0,2]-m[2,0]],
      [m[2,0]+m[0,2],m[2,1]+m[1,2],m[2,2]-m[0,0]-m[1,1],m[1,0]-m[0,1]],
      [m[2,1]-m[1,2],m[0,2]-m[2,0],m[1,0]-m[0,1],m.trace()]])/3
    w,v=np.linalg.eigh(K);q=v[:,np.argmax(w)];return q if q[3]>=0 else -q
def swing(v):
    v=unit(v);cross=np.cross([0,1,0],v);q=np.r_[cross,1+v[1]]
    return unit(q) if np.linalg.norm(q)>1e-5 else np.array([1.,0,0,0])
def matrix(q):
    x,y,z,w=q
    return np.array([[1-2*(y*y+z*z),2*(x*y-z*w),2*(x*z+y*w)],
                     [2*(x*y+z*w),1-2*(x*x+z*z),2*(y*z-x*w)],
                     [2*(x*z-y*w),2*(y*z+x*w),1-2*(x*x+y*y)]])
def solve(s,w,pole,length):
    delta=w-s;d=min(max(np.linalg.norm(delta),.1),length*1.985);axis=unit(delta);w=s+axis*d
    p=unit(pole-s-axis*np.dot(pole-s,axis))
    if np.linalg.norm(p)<.1:p=unit(np.cross(axis,[1,0,0]))
    e=s+axis*d/2+p*math.sqrt(max(0,length*length-d*d/4))
    return e,w
def smooth(a):
    kernel=np.array([1,2,3,2,1],float)/9
    padded=np.pad(a,((2,2),(0,0),(0,0)),mode='edge')
    return sum(padded[i:i+len(a)]*v for i,v in enumerate(kernel))

parser=argparse.ArgumentParser(description=__doc__)
parser.add_argument('landmarks',type=Path,help='Local PoseLandmarker world/image frames, sampled at 20 Hz')
parser.add_argument('output',type=Path,help='Output cute_dance.json')
args=parser.parse_args()
raw=json.loads(args.landmarks.read_text(encoding='utf-8'))['frames']
world=smooth(np.array([f['world'] for f in raw])[:,:,:3])
image=np.array([f['image'] for f in raw])
hipcenters=(image[:,23,:2]+image[:,24,:2])/2
hipmid=np.median(hipcenters[:,0])
frames=[]
for i,points in enumerate(world):
    center=(points[23]+points[24])/2; shoulders=(points[11]+points[12])/2
    down=unit(center-shoulders);right=unit(points[11]-points[12]);right=unit(right-down*np.dot(right,down))
    away=unit(np.cross(right,down));R=np.column_stack((right,down,away))
    # Rotate the torso around the hips and carry its shoulder pivots with it.
    bodyQ=quat(R)
    bodyPos=np.array([0.,12,0])+R@np.array([0.,-12,0])
    earline=unit(points[7]-points[8]);face=unit(points[0]-(points[7]+points[8])/2)
    yaw=np.clip(math.atan2(-face[0],-face[2]),-.8,.8)
    pitch=np.clip(math.asin(np.clip(face[1],-1,1)),-.22,.28)
    roll=np.clip(math.atan2(earline[1],earline[0]),-.3,.3)
    cx,sx=math.cos(pitch),math.sin(pitch);cy,sy=math.cos(yaw),math.sin(yaw);cz,sz=math.cos(roll),math.sin(roll)
    headQ=quat(np.array([[cz,-sz,0],[sz,cz,0],[0,0,1]])@np.array([[cy,0,sy],[0,1,0],[-sy,0,cy]])@np.array([[1,0,0],[0,cx,-sx],[0,sx,cx]]))
    qs=[bodyQ,headQ];feet=[]
    for side,a,b,c in [(-1,12,14,16),(1,11,13,15),(-1,24,26,28),(1,23,25,27)]:
        arm=a<20;length=5 if arm else 6
        s=bodyPos+R@np.array([side*5.,2,0]) if arm else np.array([0.,12,0])+R@np.array([side*2.,0,0])
        e=s+unit(points[b]-points[a])*length;w=e+unit(points[c]-points[b])*length
        if arm:
            local=R.T@(w-bodyPos);elbow=R.T@(e-bodyPos)
            # Broad block hands cannot occupy the narrow cheek/chest space of a human hand.
            if abs(local[0])<6.4 and local[1]<2.5:
                local[2]=min(local[2],-8.0)
                local[0]=side*max(2.35,abs(local[0]))
                local[1]=max(local[1],-3.5)
            elif abs(local[0])<5.8 and local[1]<12:
                local[2]=min(local[2],-6.0)
                local[0]=side*max(2.2,abs(local[0]))
            if elbow[2]>-3.5:elbow[0]=side*max(5.7,abs(elbow[0]))
            if local[2]<-5:elbow[2]=min(elbow[2],-2.7)
            e,w=solve(s,bodyPos+R@local,bodyPos+R@elbow,length)
        else:
            # Keep thick trouser legs apart while retaining the original lifted-leg direction.
            w[0]=side*max(2.2,side*w[0]);e[0]=side*max(2.15,side*e[0])
            e,w=solve(s,w,e,length)
        upper=swing(e-s);lower=swing(w-e);qs.extend([upper,lower])
        if not arm:
            lm=matrix(lower);feet.append(w[1]+2*(abs(lm[1,0])+abs(lm[1,2])))
    dx=float(np.clip((hipcenters[i,0]-hipmid)*90,-3.5,3.5))
    dy=24-max(feet)
    frames.append([*[float(x) for q in qs for x in q],dx,float(dy)])

# Quaternion sign continuity plus a short cyclic blend; runtime interpolates rotations with SLERP.
data=np.array(frames)
for j in range(10):
    k=j*4
    for i in range(1,len(data)):
        if np.dot(data[i-1,k:k+4],data[i,k:k+4])<0:data[i,k:k+4]*=-1
for i in range(len(data)-14,len(data)):
    t=(i-(len(data)-14))/13;t=t*t*(3-2*t)
    for j in range(10):
        k=j*4;target=data[0,k:k+4].copy()
        if np.dot(data[i,k:k+4],target)<0:target*=-1
        data[i,k:k+4]=unit(data[i,k:k+4]*(1-t)+target*t)
    data[i,40:]=data[i,40:]*(1-t)+data[0,40:]*t
assert np.isfinite(data).all()
output={'duration':20.0,'frames':np.round(data,6).tolist()}
dest=args.output
dest.parent.mkdir(parents=True,exist_ok=True);dest.write_text(json.dumps(output,separators=(',',':')),encoding='utf-8')
print('Retargeted',len(frames),'frames:',dest.stat().st_size,'bytes')
