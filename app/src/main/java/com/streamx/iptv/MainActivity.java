package com.streamx.iptv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.MediaItem;
import androidx.media3.common.MimeTypes;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.PlayerView;
import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.Executors;

public class MainActivity extends Activity {
    static final int BG=Color.rgb(8,11,22), CARD=Color.rgb(18,23,40), TEXT=Color.WHITE, MUTED=Color.rgb(155,165,190);
    LinearLayout root, content, categories; EditText search; TextView title; ExoPlayer player; PlayerView playerView;
    ArrayList<Channel> all=new ArrayList<>(), shown=new ArrayList<>(); HashSet<String> favorites=new HashSet<>(); SharedPreferences prefs;

    static class Channel {
        String name,group,url;
        Channel(String n,String g,String u){name=n;group=g;url=u;}
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        getWindow().setStatusBarColor(BG); getWindow().setNavigationBarColor(BG);
        prefs=getSharedPreferences("streamx",0);
        favorites.addAll(prefs.getStringSet("fav",new HashSet<>()));
        seed(); buildHome();
    }

    int dp(float v){return (int)(v*getResources().getDisplayMetrics().density+.5f);}
    TextView tv(String s,float size,int color){
        TextView t=new TextView(this); t.setText(s); t.setTextSize(size); t.setTextColor(color);
        t.setGravity(Gravity.CENTER_VERTICAL); t.setPadding(dp(12),0,dp(12),0); return t;
    }
    GradientDrawable bg(int color,float r){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(r));return d;}

    void seed(){
        String demo="https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8";
        all.add(new Channel("قناة التجربة HD","تجريبي",demo));
        all.add(new Channel("Mux Big Buck Bunny","تجريبي",demo));
        all.add(new Channel("Apple HLS Demo","تجريبي","https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"));
    }

    void base(){
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setBackgroundColor(BG);
        root.setPadding(dp(18),dp(14),dp(18),dp(10)); setContentView(root);

        LinearLayout bar=new LinearLayout(this); bar.setGravity(Gravity.CENTER_VERTICAL);
        TextView logo=tv("STREAMX",24,TEXT); logo.setTypeface(null,1);
        bar.addView(logo,new LinearLayout.LayoutParams(dp(150),dp(52)));
        title=tv("بث مباشر",18,TEXT); title.setGravity(Gravity.CENTER);
        bar.addView(title,new LinearLayout.LayoutParams(0,dp(52),1));
        TextView add=tv("＋ إضافة قائمة",15,TEXT); add.setGravity(Gravity.CENTER); add.setBackground(bg(CARD,18));
        add.setOnClickListener(v->showAdd()); bar.addView(add,new LinearLayout.LayoutParams(dp(150),dp(44))); root.addView(bar);

        search=new EditText(this); search.setHint("ابحث عن قناة..."); search.setHintTextColor(MUTED); search.setTextColor(TEXT);
        search.setSingleLine(true); search.setPadding(dp(14),0,dp(14),0); search.setBackground(bg(CARD,18));
        search.addTextChangedListener(new android.text.TextWatcher(){
            public void beforeTextChanged(CharSequence s,int a,int c,int d){}
            public void onTextChanged(CharSequence s,int a,int b,int c){filter(s.toString());}
            public void afterTextChanged(android.text.Editable e){}
        });
        root.addView(search,new LinearLayout.LayoutParams(-1,dp(46)));

        categories=new LinearLayout(this); categories.setOrientation(LinearLayout.HORIZONTAL);
        root.addView(categories,new LinearLayout.LayoutParams(-1,dp(56)));
        addCat("الكل"); addCat("تجريبي"); addCat("المفضلة");

        ScrollView sv=new ScrollView(this); content=new LinearLayout(this); content.setOrientation(LinearLayout.VERTICAL);
        sv.addView(content); root.addView(sv,new LinearLayout.LayoutParams(-1,0,1));
    }

    void addCat(String c){
        TextView x=tv(c,14,TEXT); x.setGravity(Gravity.CENTER); x.setBackground(bg(CARD,18));
        x.setOnClickListener(v->{title.setText(c); shown.clear();
            if(c.equals("المفضلة")) for(Channel ch:all)if(favorites.contains(ch.name))shown.add(ch);
            else if(c.equals("الكل")) shown.addAll(all);
            else for(Channel ch:all)if(ch.group.equals(c))shown.add(ch);
            render();
        });
        LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(dp(105),dp(38)); p.setMargins(0,0,dp(8),0); categories.addView(x,p);
    }

    void buildHome(){base();shown.clear();shown.addAll(all);render();}
    void filter(String q){
        shown.clear(); String z=q.trim().toLowerCase(Locale.ROOT);
        for(Channel c:all) if(z.isEmpty()||c.name.toLowerCase(Locale.ROOT).contains(z)||c.group.toLowerCase(Locale.ROOT).contains(z))shown.add(c);
        render();
    }

    void render(){
        if(content==null)return; content.removeAllViews();
        TextView h=tv("القنوات",20,TEXT); h.setTypeface(null,1); content.addView(h,new LinearLayout.LayoutParams(-1,dp(48)));
        for(Channel c:shown){
            LinearLayout card=new LinearLayout(this); card.setGravity(Gravity.CENTER_VERTICAL); card.setPadding(dp(8),0,dp(8),0); card.setBackground(bg(CARD,18));
            TextView name=tv(c.name+"\n"+c.group,15,TEXT); card.addView(name,new LinearLayout.LayoutParams(0,dp(72),1));
            TextView fav=tv(favorites.contains(c.name)?"★":"☆",27,Color.rgb(255,205,80)); fav.setGravity(Gravity.CENTER);
            fav.setOnClickListener(v->{toggleFav(c);fav.setText(favorites.contains(c.name)?"★":"☆");});
            card.addView(fav,new LinearLayout.LayoutParams(dp(60),dp(60))); card.setOnClickListener(v->play(c));
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(-1,dp(78)); lp.setMargins(0,0,0,dp(10)); content.addView(card,lp);
        }
    }

    void toggleFav(Channel c){
        if(favorites.contains(c.name))favorites.remove(c.name); else favorites.add(c.name);
        prefs.edit().putStringSet("fav",favorites).apply();
    }

    void play(Channel c){
        root.removeAllViews(); root.setPadding(0,0,0,0);
        playerView=new PlayerView(this); playerView.setUseController(true); root.addView(playerView,new LinearLayout.LayoutParams(-1,0,1));
        LinearLayout bottom=new LinearLayout(this); bottom.setPadding(dp(14),dp(8),dp(14),dp(8)); bottom.setBackgroundColor(Color.rgb(5,7,14));
        TextView back=tv("‹  العودة",16,TEXT); back.setGravity(Gravity.CENTER); back.setBackground(bg(CARD,16));
        back.setOnClickListener(v->{releasePlayer();buildHome();}); bottom.addView(back,new LinearLayout.LayoutParams(dp(120),dp(48)));
        TextView n=tv(c.name,17,TEXT); n.setGravity(Gravity.CENTER); bottom.addView(n,new LinearLayout.LayoutParams(0,dp(48),1)); root.addView(bottom);
        player=new ExoPlayer.Builder(this).build(); playerView.setPlayer(player);
        MediaItem item=new MediaItem.Builder().setUri(Uri.parse(c.url)).setMimeType(MimeTypes.APPLICATION_M3U8).build();
        player.setMediaItem(item); player.prepare(); player.play();
    }

    void releasePlayer(){if(player!=null){player.release();player=null;}if(playerView!=null)playerView.setPlayer(null);}
    @Override protected void onDestroy(){releasePlayer();super.onDestroy();}

    void showAdd(){
        final EditText input=new EditText(this); input.setHint("https://example.com/playlist.m3u"); input.setSingleLine(true);
        input.setTextColor(TEXT); input.setHintTextColor(MUTED);
        LinearLayout box=new LinearLayout(this); box.setPadding(dp(20),dp(8),dp(20),0); box.addView(input,new LinearLayout.LayoutParams(-1,dp(54)));
        AlertDialog d=new AlertDialog.Builder(this).setTitle("إضافة قائمة IPTV")
            .setMessage("أدخل رابط M3U تملكه أو لديك تصريح لاستخدامه.").setView(box)
            .setNegativeButton("إلغاء",null).setPositiveButton("استيراد",null).create();
        d.setOnShowListener(x->d.getButton(-1).setOnClickListener(v->{String u=input.getText().toString().trim();if(u.isEmpty())return;d.dismiss();importM3U(u);}));
        d.show();
    }

    void importM3U(String url){
        Toast.makeText(this,"جاري استيراد القائمة...",Toast.LENGTH_SHORT).show();
        Executors.newSingleThreadExecutor().execute(()->{
            try{
                HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
                c.setConnectTimeout(10000); c.setReadTimeout(20000); c.setRequestProperty("User-Agent","StreamX/1.0");
                BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream()));
                String line,name="قناة",group="مستورد"; ArrayList<Channel> add=new ArrayList<>();
                while((line=r.readLine())!=null){
                    line=line.trim();
                    if(line.startsWith("#EXTINF")){
                        int comma=line.indexOf(','); name=comma>=0?line.substring(comma+1).trim():"قناة";
                        int g=line.indexOf("group-title=\""); if(g>=0){int s=g+13,e=line.indexOf('"',s);if(e>s)group=line.substring(s,e);}
                    } else if(!line.isEmpty()&&!line.startsWith("#")&&(line.startsWith("http://")||line.startsWith("https://"))){
                        add.add(new Channel(name,group,line)); name="قناة"; group="مستورد";
                    }
                }
                r.close(); c.disconnect();
                runOnUiThread(()->{all.addAll(add);shown.clear();shown.addAll(all);render();
                    Toast.makeText(this,"تمت إضافة "+add.size()+" قناة",Toast.LENGTH_LONG).show();});
            }catch(Exception e){runOnUiThread(()->Toast.makeText(this,"تعذر استيراد القائمة: "+e.getMessage(),Toast.LENGTH_LONG).show());}
        });
    }
}