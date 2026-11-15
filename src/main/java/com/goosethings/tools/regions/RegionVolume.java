package com.goosethings.tools.regions;

import java.util.List;

/** An X/Z simple polygon extruded through [minY,maxY). Boundary edges are included. */
public record RegionVolume(double minY,double maxY,List<Point> points,boolean blockAligned) {
    public RegionVolume(double minY,double maxY,List<Point> points){this(minY,maxY,points,false);}
    public record Point(double x,double z) {
        public Point { if(!Double.isFinite(x)||!Double.isFinite(z)||Math.abs(x)>30_000_000||Math.abs(z)>30_000_000)throw new IllegalArgumentException("coordinate"); }
    }
    private static final double EPS=1e-7;
    public RegionVolume {
        points=List.copyOf(points);
        if(blockAligned&&(points.stream().anyMatch(p->p.x()!=Math.floor(p.x())||p.z()!=Math.floor(p.z()))||span(points,true)>512||span(points,false)>512))throw new IllegalArgumentException("grid");
        if(!Double.isFinite(minY)||!Double.isFinite(maxY)||minY< -2048||maxY>2048||minY>=maxY)throw new IllegalArgumentException("height");
        if(points.size()<3||points.size()>64)throw new IllegalArgumentException("vertices");
        double area=0;
        for(int i=0;i<points.size();i++){
            Point a=points.get(i),b=points.get((i+1)%points.size());
            if(distance(a,b)<EPS)throw new IllegalArgumentException("duplicate_vertex");
            area+=a.x*b.z-b.x*a.z;
            for(int j=i+1;j<points.size();j++){
                if(j==i+1||i==0&&j==points.size()-1)continue;
                if(intersects(a,b,points.get(j),points.get((j+1)%points.size())))throw new IllegalArgumentException("self_intersection");
            }
        }
        if(Math.abs(area)<.01)throw new IllegalArgumentException("zero_area");
        // Adjacent backtracking edges overlap even though they share an allowed endpoint.
        for(int i=0;i<points.size();i++){Point a=points.get((i+points.size()-1)%points.size()),b=points.get(i),c=points.get((i+1)%points.size());
            if(Math.abs(cross(a,b,c))<EPS&&(a.x-b.x)*(c.x-b.x)+(a.z-b.z)*(c.z-b.z)>EPS)throw new IllegalArgumentException("overlap");
        }
    }
    public boolean contains(double x,double y,double z,double grace){
        if(y<minY||y>=maxY)return false;
        if(blockAligned){x=Math.floor(x);z=Math.floor(z);grace=0;}
        boolean in=false;Point p=new Point(x,z);
        for(int i=0,j=points.size()-1;i<points.size();j=i++){
            Point a=points.get(j),b=points.get(i);
            if(edgeDistance(p,a,b)<=Math.max(EPS,grace))return true;
            if((a.z>z)!=(b.z>z)&&x<(b.x-a.x)*(z-a.z)/(b.z-a.z)+a.x)in=!in;
        }return in;
    }
    public double minX(){return points.stream().mapToDouble(Point::x).min().orElseThrow();}
    public double maxX(){return points.stream().mapToDouble(Point::x).max().orElseThrow()+(blockAligned?1:0);}
    public double minZ(){return points.stream().mapToDouble(Point::z).min().orElseThrow();}
    public double maxZ(){return points.stream().mapToDouble(Point::z).max().orElseThrow()+(blockAligned?1:0);}
    public static RegionVolume box(double x1,double x2,double y1,double y2,double z1,double z2){
        return new RegionVolume(y1,y2,List.of(new Point(x1,z1),new Point(x2,z1),new Point(x2,z2),new Point(x1,z2)));
    }
    private static double span(List<Point> points,boolean x){return points.stream().mapToDouble(p->x?p.x:p.z).max().orElse(0)-points.stream().mapToDouble(p->x?p.x:p.z).min().orElse(0);}
    private static double cross(Point a,Point b,Point c){return (b.x-a.x)*(c.z-a.z)-(b.z-a.z)*(c.x-a.x);}
    private static double distance(Point a,Point b){return Math.hypot(a.x-b.x,a.z-b.z);}
    private static double edgeDistance(Point p,Point a,Point b){double dx=b.x-a.x,dz=b.z-a.z,t=((p.x-a.x)*dx+(p.z-a.z)*dz)/(dx*dx+dz*dz);t=Math.clamp(t,0,1);return Math.hypot(p.x-a.x-t*dx,p.z-a.z-t*dz);}
    private static boolean intersects(Point a,Point b,Point c,Point d){
        if(edgeDistance(a,c,d)<EPS||edgeDistance(b,c,d)<EPS||edgeDistance(c,a,b)<EPS||edgeDistance(d,a,b)<EPS)return true;
        return Math.signum(cross(a,b,c))*Math.signum(cross(a,b,d))<0&&Math.signum(cross(c,d,a))*Math.signum(cross(c,d,b))<0;
    }
}
