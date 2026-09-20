package com.betwaz.fxai;

import android.app.*;
import android.os.*;
import android.graphics.Color;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.net.*;
import org.json.*;

public class MainActivity extends Activity {
    private static final String API="https://betwaz-fx-ai-backend.onrender.com";
    LinearLayout root; TextView status, price, decision; Spinner symbol;
    Button scan, stop;
    String[] symbols={"XAU/USD","WTI/USD","USD/ZAR","BTC/USD","ETH/USD","SOL/USD"};

    int dp(float v){ return (int)(v*getResources().getDisplayMetrics().density+0.5f); }
    TextView tv(String s,int size){ TextView t=new TextView(this); t.setText(s); t.setTextColor(Color.WHITE); t.setTextSize(size); t.setPadding(dp(12),dp(8),dp(12),dp(8)); return t; }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL); root.setPadding(dp(14),dp(12),dp(14),dp(18)); root.setBackgroundColor(Color.rgb(7,26,61));
        ScrollView scroll=new ScrollView(this); scroll.addView(root); setContentView(scroll);

        TextView brand=tv("BETWAZ FX AI",26); brand.setTypeface(null,1); root.addView(brand);
        TextView sub=tv("AI intelligence • decision engine • risk control",11); sub.setTextColor(Color.rgb(145,201,237)); root.addView(sub);

        status=tv("SAFE MODE • LIVE TRADING DISABLED",12); status.setTextColor(Color.rgb(32,240,181)); root.addView(status);

        root.addView(tv("CURRENT MARKET",12));
        symbol=new Spinner(this); ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,symbols); symbol.setAdapter(a); root.addView(symbol);

        price=tv("Price: loading…",18); root.addView(price);
        decision=tv("AI DECISION: WAIT",24); decision.setTextColor(Color.YELLOW); root.addView(decision);

        scan=new Button(this); scan.setText("SCAN MARKET WITH AI"); scan.setOnClickListener(v->scan()); root.addView(scan);
        stop=new Button(this); stop.setText("EMERGENCY STOP"); stop.setTextColor(Color.WHITE); stop.setBackgroundColor(Color.rgb(165,45,70)); stop.setOnClickListener(v->emergencyStop()); root.addView(stop);

        Button refresh=new Button(this); refresh.setText("REFRESH MARKET"); refresh.setOnClickListener(v->refreshPrice()); root.addView(refresh);
        root.addView(tv("Practice/demo interface only. Live trading remains disabled in the backend.",11));

        refreshPrice();
    }

    void refreshPrice(){ final String s=(String)symbol.getSelectedItem(); new Thread(()->{
        try{
            JSONObject d=get(API+"/api/market-data?symbol="+URLEncoder.encode(s,"UTF-8"));
            String p=d.optString("price","—");
            runOnUiThread(()->price.setText("Price: "+p+" • "+(d.optJSONObject("market_status")!=null && d.optJSONObject("market_status").optBoolean("open")?"MARKET OPEN":"MARKET STATUS UNKNOWN")));
        }catch(Exception e){runOnUiThread(()->price.setText("Market data unavailable: "+e.getMessage()));}
    }).start();}

    void scan(){ final String s=(String)symbol.getSelectedItem(); scan.setEnabled(false); decision.setText("AI DECISION: SCANNING…"); new Thread(()->{
        try{
            JSONObject d=get(API+"/api/ai-test?symbol="+URLEncoder.encode(s,"UTF-8"));
            JSONObject a=d.optJSONObject("analysis"); if(a==null)a=d;
            String side=a.optString("side","WAIT"); int q=(int)(Math.max(0,Math.min(1,a.optDouble("confidence",0)))*100);
            String text="AI DECISION: "+side+" • "+q+"%\nEntry: "+a.optString("entry","—")+"  Stop: "+a.optString("stop","—")+"  Target: "+a.optString("target","—");
            runOnUiThread(()->{decision.setText(text); decision.setTextColor(side.equals("BUY")?Color.rgb(32,240,181):side.equals("SELL")?Color.rgb(255,79,104):Color.YELLOW); scan.setEnabled(true);});
        }catch(Exception e){runOnUiThread(()->{decision.setText("AI DECISION: WAIT\nScan failed: "+e.getMessage());scan.setEnabled(true);});}
    }).start();}

    void emergencyStop(){ status.setText("SAFE MODE • EMERGENCY STOP REQUESTED"); Toast.makeText(this,"Backend emergency-stop requires operator authorization.",Toast.LENGTH_LONG).show(); }

    JSONObject get(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection(); c.setConnectTimeout(15000); c.setReadTimeout(20000); c.setRequestMethod("GET");
        BufferedReader r=new BufferedReader(new InputStreamReader(c.getInputStream())); StringBuilder sb=new StringBuilder(); String line; while((line=r.readLine())!=null)sb.append(line); r.close(); return new JSONObject(sb.toString());
    }
}
