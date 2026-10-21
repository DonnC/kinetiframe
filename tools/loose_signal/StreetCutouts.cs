using System;
using System.Drawing;
using System.Drawing.Imaging;
using System.IO;
using System.Collections.Generic;

// Mechanical atlas extraction only: source pixels, outlines and alpha are not repainted.
public static class StreetCutouts {
    public static void ExtractDirectional(string source, string characterRoot, string part, string version="v5") {
        string[] characters={"tavi","mira","kuda"};
        string[] views={"front","back","profile","three_quarter"};
        using(var atlas=new Bitmap(source)) for(int row=0;row<3;row++) for(int col=0;col<4;col++) {
            int x=col*atlas.Width/4,right=(col+1)*atlas.Width/4;
            int y=row*atlas.Height/3,bottom=(row+1)*atlas.Height/3;
            string output=Path.Combine(characterRoot,characters[row],"full-body/"+version+"/cutouts");
            Directory.CreateDirectory(output);
            using(var cell=atlas.Clone(new Rectangle(x,y,right-x,bottom-y),PixelFormat.Format32bppArgb))
            using(var cutout=Trim(cell)) {
                cutout.Save(Path.Combine(output,views[col]+"-"+part+".png"),ImageFormat.Png);
                if(part.StartsWith("hand-") && views[col]!="profile") {
                    cutout.RotateFlip(RotateFlipType.RotateNoneFlipX);
                    cutout.Save(Path.Combine(output,views[col]+"-"+part+"-left.png"),ImageFormat.Png);
                }
            }
        }
    }
    public static void ExtractDirectionalBody(string source, string output) {
        Directory.CreateDirectory(output);
        string[] views={"front","back","profile","three_quarter"};
        using(var atlas=new Bitmap(source)) for(int row=0;row<2;row++) for(int col=0;col<4;col++) {
            int x=col*atlas.Width/4,right=(col+1)*atlas.Width/4;
            int y=row*atlas.Height/2,bottom=(row+1)*atlas.Height/2;
            using(var cell=atlas.Clone(new Rectangle(x,y,right-x,bottom-y),PixelFormat.Format32bppArgb))
            using(var cutout=Trim(cell)) cutout.Save(Path.Combine(output,views[col]+(row==0?"-head.png":"-torso.png")),ImageFormat.Png);
        }
    }
    public static void ExtractViews(string source, string output, bool includeProfile) {
        Directory.CreateDirectory(output);
        string[] views=includeProfile ? new[]{"front","profile","back"} : new[]{"front","back"};
        using(var atlas=new Bitmap(source)) for(int row=0;row<2;row++) for(int col=0;col<views.Length;col++) {
            int x=col*atlas.Width/views.Length,right=(col+1)*atlas.Width/views.Length;
            int y=row*atlas.Height/2,bottom=(row+1)*atlas.Height/2;
            using(var cell=atlas.Clone(new Rectangle(x,y,right-x,bottom-y),PixelFormat.Format32bppArgb))
            using(var cutout=Trim(cell)) cutout.Save(Path.Combine(output,views[col]+(row==0?"-head.png":"-torso.png")),ImageFormat.Png);
        }
    }
    public static void ExtractContinuous(string limbsSource,string torsoSource,string characterRoot) {
        string[] characters={"tavi","kuda","mira"};
        foreach(string source in new[]{limbsSource,torsoSource}) using(var atlas=new Bitmap(source)) {
            int split=(source==limbsSource ? 470 : 472)*atlas.Height/1024;
            for(int row=0;row<2;row++) for(int column=0;column<3;column++) {
                int x=column*atlas.Width/3,right=(column+1)*atlas.Width/3;
                int y=row==0 ? 0 : split,bottom=row==0 ? split : atlas.Height;
                string name=source==limbsSource ? (row==0?"sleeve":"trouser-leg") : (row==0?"torso":"profile-torso");
                string output=Path.Combine(characterRoot,characters[column],"full-body/v3/cutouts");
                Directory.CreateDirectory(output);
                using(var cell=atlas.Clone(new Rectangle(x,y,right-x,bottom-y),PixelFormat.Format32bppArgb))
                using(var cutout=Trim(cell)) cutout.Save(Path.Combine(output,name+".png"),ImageFormat.Png);
            }
        }
    }
    static readonly string[] Names = { "head", "torso", "upper-arm", "forearm", "hand-rest",
        "hand-palm", "hand-point", "hand-fist", "thigh", "shin", "foot", "profile-head",
        "profile-torso", "eyes-rest", "eyes-angry", "eyes-laugh", "mouth-rest", "mouth-open",
        "mouth-frown", "eyes-surprise" };
    public static void Extract(string source, string output) {
        Directory.CreateDirectory(output);
        using (var atlas = new Bitmap(source)) {
            for (int i = 0; i < Names.Length; i++) {
                // Generated gutters are not a perfect grid. Explicit reviewed source regions
                // keep whole silhouettes and do not leak adjacent sleeves into expressions.
                int[] columns={0,290,575,790,1020,1280};
                bool mira=source.IndexOf("mira",StringComparison.OrdinalIgnoreCase)>=0;
                int[] rows=mira ? new[]{0,335,635,990,1280} : new[]{0,375,695,1000,1280};
                int x=columns[i%5]*atlas.Width/1280, y=rows[i/5]*atlas.Height/1280;
                int right=columns[i%5+1]*atlas.Width/1280, bottom=rows[i/5+1]*atlas.Height/1280;
                if(i==13 || i==14) y=760*atlas.Height/1280;
                if(i==10 && mira) right=250*atlas.Width/1280;
                using (var cell = atlas.Clone(new Rectangle(x,y,right-x,bottom-y),PixelFormat.Format32bppArgb))
                using (var trimmed = Trim(cell)) {
                    trimmed.Save(Path.Combine(output,Names[i]+".png"),ImageFormat.Png);
                    if (Names[i].StartsWith("eyes-")) {
                        using(var eye = trimmed.Clone(new Rectangle(trimmed.Width/2,0,trimmed.Width-trimmed.Width/2,trimmed.Height),PixelFormat.Format32bppArgb))
                        using(var one = Trim(eye)) one.Save(Path.Combine(output,"profile-"+Names[i]+".png"),ImageFormat.Png);
                    }
                }
            }
        }
    }
    public static void ExtractTorsos(string source,string characterRoot) {
        string[] characters={"tavi","kuda","mira"};
        using(var atlas=new Bitmap(source)) for(int row=0;row<2;row++) for(int column=0;column<3;column++) {
            int x=column*atlas.Width/3,right=(column+1)*atlas.Width/3;
            int y=row*atlas.Height/2,bottom=(row+1)*atlas.Height/2;
            using(var cell=atlas.Clone(new Rectangle(x,y,right-x,bottom-y),PixelFormat.Format32bppArgb))
            using(var cutout=Trim(cell)) cutout.Save(Path.Combine(characterRoot,characters[column],"full-body/v2/cutouts",row==0?"torso.png":"profile-torso.png"),ImageFormat.Png);
        }
    }
    static Bitmap Trim(Bitmap image) {
        // Ignore detached generation dust and neighbouring tiny outline fragments. Keep all
        // substantial disconnected facial marks (eyebrows/eyes), not merely the largest blob.
        var seen=new bool[image.Width*image.Height];var components=new List<List<Point>>();int largest=0;
        for(int y=0;y<image.Height;y++) for(int x=0;x<image.Width;x++) {
            if(seen[y*image.Width+x] || image.GetPixel(x,y).A<24) continue;
            var points=new List<Point>();var queue=new Queue<Point>();queue.Enqueue(new Point(x,y));seen[y*image.Width+x]=true;
            while(queue.Count>0) {
                var point=queue.Dequeue();points.Add(point);
                for(int dy=-1;dy<=1;dy++) for(int dx=-1;dx<=1;dx++) {
                    int nx=point.X+dx,ny=point.Y+dy;
                    if(nx<0||ny<0||nx>=image.Width||ny>=image.Height||seen[ny*image.Width+nx]||image.GetPixel(nx,ny).A<24) continue;
                    seen[ny*image.Width+nx]=true;queue.Enqueue(new Point(nx,ny));
                }
            }
            components.Add(points);largest=Math.Max(largest,points.Count);
        }
        int left=image.Width,top=image.Height,right=-1,bottom=-1;
        foreach(var component in components) if(component.Count>=Math.Max(6,largest*.025)) foreach(var point in component) {
            left=Math.Min(left,point.X);top=Math.Min(top,point.Y);right=Math.Max(right,point.X);bottom=Math.Max(bottom,point.Y);
        }
        if(right<left) throw new InvalidDataException("Empty atlas cell");
        // One extra source pixel retains edge antialiasing without carrying distant dust.
        left=Math.Max(0,left-1);top=Math.Max(0,top-1);right=Math.Min(image.Width-1,right+1);bottom=Math.Min(image.Height-1,bottom+1);
        return image.Clone(new Rectangle(left,top,right-left+1,bottom-top+1),PixelFormat.Format32bppArgb);
    }
}
