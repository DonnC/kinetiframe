using System;
using System.Drawing;
using System.Drawing.Drawing2D;
using System.Drawing.Imaging;
using System.IO;

// Authoring-only atlas slicing and cutout composition. Never redraws character geometry.
// Keeping source art separate from motion lets a corrected atlas rebuild every export.
public static class TaviProduction {
    public const int Width = 1280, Height = 720, Frames = 96, Fps = 12;
    static Bitmap Blank(int w, int h) { return new Bitmap(w,h,PixelFormat.Format32bppArgb); }
    static Graphics Canvas(Bitmap b) {
        var g=Graphics.FromImage(b); g.InterpolationMode=InterpolationMode.HighQualityBicubic;
        g.CompositingQuality=CompositingQuality.HighQuality; g.PixelOffsetMode=PixelOffsetMode.HighQuality;
        return g;
    }
    public static Bitmap Slice(string path, Rectangle region) {
        using(var atlas=new Bitmap(path)) {
            int left=region.Right,top=region.Bottom,right=region.Left,bottom=region.Top;
            for(int y=region.Top;y<region.Bottom;y++) for(int x=region.Left;x<region.Right;x++) {
                // Ignore isolated near-transparent generation noise when measuring a
                // sprite's crop. Keep the original edge pixels inside the crop intact.
                if(atlas.GetPixel(x,y).A<32) continue;
                left=Math.Min(left,x);top=Math.Min(top,y);right=Math.Max(right,x);bottom=Math.Max(bottom,y);
            }
            if(right<left) throw new InvalidDataException("Empty atlas cell: "+path);
            return atlas.Clone(new Rectangle(left,top,right-left+1,bottom-top+1),PixelFormat.Format32bppArgb);
        }
    }
    public static void Stamp(Bitmap source,string destination,int w,int h,int x,int y) {
        using(var b=Blank(512,512)) using(var g=Canvas(b)) {
            g.DrawImage(source,x,y,w,h);b.Save(destination,ImageFormat.Png);
        }
    }
    static double Ease(double t) { t=Math.Max(0,Math.Min(1,t));return t*t*(3-2*t); }
    static double Mix(double a,double b,double t) { return a+(b-a)*t; }
    // Wrist goals describe readable silhouettes. Elbows are authored too, avoiding an
    // ambiguous IK branch flip. Cubic timing gives anticipation and a quiet held pose.
    static readonly double[][] Keys={
        new double[]{0, 150,540,250,580, 445,540,375,580, 0,0},
        new double[]{1, 150,535,250,580, 455,500,510,455, -15,3},
        new double[]{2.3,150,535,250,580, 455,495,510,455, -15,3},
        new double[]{2.8,150,535,250,580, 475,470,555,395, -10,1},
        new double[]{4,150,535,250,580, 475,470,555,395, -10,1},
        new double[]{4.5,150,535,250,580, 455,465,455,450, 0,2},
        new double[]{4.8,150,535,250,580, 450,545,420,588, 0,2},
        new double[]{5.2,150,535,250,580, 450,530,420,568, 0,2},
        new double[]{5.6,150,535,250,580, 450,545,420,580, 0,2},
        new double[]{6.2,150,510,220,465, 450,510,390,465, -12,3},
        new double[]{7.3,150,510,220,465, 450,510,390,465, -12,3},
        new double[]{7.9,150,540,250,580, 445,540,375,580, 0,0}
    };
    static double[] Pose(double t) {
        int k=0;while(k<Keys.Length-2 && t>Keys[k+1][0]) k++;
        var a=Keys[k];var b=Keys[k+1];double u=Ease((t-a[0])/(b[0]-a[0]));var p=new double[a.Length];
        for(int i=1;i<p.Length-1;i++) p[i]=Mix(a[i],b[i],u);
        p[p.Length-1]=t>=b[0]?b[b.Length-1]:a[a.Length-1];return p;
    }
    static void Segment(Graphics g,Bitmap sprite,double ax,double ay,double bx,double by,float width) {
        var state=g.Save();g.TranslateTransform((float)ax,(float)ay);
        g.RotateTransform((float)(Math.Atan2(by-ay,bx-ax)*180/Math.PI-90));
        double length=Math.Sqrt((bx-ax)*(bx-ax)+(by-ay)*(by-ay));
        // Round cutout ends overlap at the elbow; no generated rectangular joints.
        var destination=new RectangleF(-width/2,-width/3,width,(float)length+width*2/3);
        g.DrawImage(sprite,destination);
        g.Restore(state);
    }
    static void Hand(Graphics g,Bitmap sprite,double x,double y,double angle) {
        var state=g.Save();g.TranslateTransform((float)x,(float)y);g.RotateTransform((float)angle);
        g.DrawImage(sprite,-36,-88,72,90);g.Restore(state);
    }
    public static void Build(string exports,string stagePath,string framesPath,string previewsPath) {
        Directory.CreateDirectory(framesPath);Directory.CreateDirectory(previewsPath);
        string[] names={"head-hair","torso-tie","upper-sleeve","forearm","eyes-neutral","eyes-skeptical","eyes-laugh","eyes-surprised",
            "mouth-smile","mouth-small","mouth-open","mouth-o","mouth-laugh","mouth-frown","hand-relaxed","hand-point","hand-fist","hand-palm","desk"};
        var images=new Bitmap[names.Length];
        try {
            for(int i=0;i<names.Length;i++) {
                var matches=Directory.GetFiles(exports,names[i]+"-cutout.png",SearchOption.AllDirectories);
                if(matches.Length!=1) throw new InvalidOperationException("Expected one registered cutout: "+names[i]);
                images[i]=new Bitmap(matches[0]);
            }
            using(var stage=new Bitmap(stagePath)) for(int frame=0;frame<Frames;frame++) {
                double t=frame/(double)Fps;var pose=Pose(t);
                int eye=t>=6 && t<7.5?2:t>=4 && t<5.8?1:0;
                // Deliberate held mouth swaps, not a pretend phoneme solver. Retiming is
                // left to the animator once real dialogue exists. Quiet bookend holds.
                int mouth=t<.6 || t>7.5?0:t>=6?4:t>=4.7 && t<5.2?3:(frame/3)%3;
                float bob=(float)(Math.Sin(t*2.8)*1.5 + (t>=6 && t<7.5?Math.Sin(t*13)*2:0));
                using(var body=Blank(Width,Height)) using(var hands=Blank(Width,Height))
                using(var eyes=Blank(Width,Height)) using(var mouths=Blank(Width,Height)) using(var desk=Blank(Width,Height)) {
                    using(var g=Canvas(body)) {
                        Segment(g,images[2],190,448,pose[1],pose[2],63);
                        Segment(g,images[3],pose[1],pose[2],pose[3],pose[4],58);
                        Segment(g,images[2],410,448,pose[5],pose[6],63);
                        Segment(g,images[3],pose[5],pose[6],pose[7],pose[8],58);
                        g.DrawImage(images[1],158,402+bob,286,170);
                        g.DrawImage(images[0],190,150+bob,220,266);
                    }
                    using(var g=Canvas(hands)) {
                        Hand(g,images[14],pose[3],pose[4],t>=6 && t<7.5?12:0);
                        int gesture=(int)pose[10];Hand(g,images[gesture==1?15:gesture==2?16:gesture==3?17:14],pose[7],pose[8],pose[9]);
                    }
                    using(var g=Canvas(eyes)) g.DrawImage(images[4+eye],243,282+bob,98,70);
                    using(var g=Canvas(mouths)) {
                        int mh=mouth==0 || mouth==5?6:mouth==3?35:30;
                        g.DrawImage(images[8+mouth],269,374-mh/2+bob,48,mh);
                    }
                    using(var g=Canvas(desk)) g.DrawImage(images[18],60,550,850,146);
                    var layers=new Bitmap[]{body,desk,hands,eyes,mouths};
                    string[] ids={"ls-body","ls-desk","ls-hands","ls-eyes","ls-mouths"};
                    for(int i=0;i<layers.Length;i++) layers[i].Save(Path.Combine(framesPath,"frame_v2_"+ids[i]+"_frame_"+frame+".png"),ImageFormat.Png);
                    using(var composite=Blank(Width,Height)) using(var g=Canvas(composite)) {
                        g.DrawImage(stage,0,0,Width,Height);foreach(var layer in layers) g.DrawImageUnscaled(layer,0,0);
                        composite.Save(Path.Combine(previewsPath,"frame-"+frame.ToString("D3")+".png"),ImageFormat.Png);
                    }
                }
            }
        } finally { foreach(var image in images) if(image!=null) image.Dispose(); }
    }
}
