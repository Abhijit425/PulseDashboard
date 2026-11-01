import math, sys
from PIL import Image, ImageDraw, ImageFilter
S=1024
img=Image.new("RGBA",(S,S),(0,0,0,0))
# rounded-square background with vertical gradient
bg=Image.new("RGBA",(S,S))
d=ImageDraw.Draw(bg)
for y in range(S):
    t=y/S
    c=(int(10+8*t),int(16+10*t),int(34+12*t),255)
    d.line([(0,y),(S,y)],fill=c)
mask=Image.new("L",(S,S),0); ImageDraw.Draw(mask).rounded_rectangle([40,40,S-40,S-40],radius=230,fill=255)
img.paste(bg,(0,0),mask)
# glow
glow=Image.new("RGBA",(S,S),(0,0,0,0)); gd=ImageDraw.Draw(glow)
cx=cy=S//2; R=300; W=70
def col(t):
    stops=[(0,(34,211,238)),(0.5,(139,92,246)),(1,(236,72,153))]
    for i in range(len(stops)-1):
        a,ca=stops[i]; b,cb=stops[i+1]
        if a<=t<=b:
            u=(t-a)/(b-a); return tuple(int(ca[k]+(cb[k]-ca[k])*u) for k in range(3))
    return stops[-1][1]
# track
gd.ellipse([cx-R,cy-R,cx+R,cy+R],outline=(255,255,255,28),width=W)
frac=0.78; N=600
for i in range(N):
    t=i/N
    if t>frac: break
    a=math.radians(-90+360*t)
    x=cx+R*math.cos(a)-0; y=cy+R*math.sin(a)
    c=col(t/frac)
    gd.ellipse([x-W/2,y-W/2,x+W/2,y+W/2],fill=c+(255,))
blur=glow.filter(ImageFilter.GaussianBlur(28))
img=Image.alpha_composite(img,Image.composite(blur,Image.new("RGBA",(S,S),(0,0,0,0)),mask))
img=Image.alpha_composite(img,glow)
# pulse line
d=ImageDraw.Draw(img); s=190
pts=[(cx-s,cy),(cx-s*0.45,cy),(cx-s*0.18,cy-s*0.62),(cx+s*0.14,cy+s*0.62),(cx+s*0.38,cy-s*0.2),(cx+s*0.52,cy),(cx+s,cy)]
d.line(pts,fill=(255,255,255,255),width=44,joint="curve")
for p in (pts[0],pts[-1]): d.ellipse([p[0]-22,p[1]-22,p[0]+22,p[1]+22],fill=(255,255,255,255))
for name,px in [("mdpi",48),("hdpi",72),("xhdpi",96),("xxhdpi",144),("xxxhdpi",192)]:
    img.resize((px,px),Image.LANCZOS).save(f"{sys.argv[1]}/res/mipmap-{name}/ic_launcher.png")
img.resize((512,512),Image.LANCZOS).save(f"{sys.argv[1]}/icon_512.png")
