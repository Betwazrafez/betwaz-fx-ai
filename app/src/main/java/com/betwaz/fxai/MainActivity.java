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

    TextView tv(String s,int size){
        TextView t=new TextView(this);
        t.setText(s);
        t.setTextColor(Color.WHITE);
        t.setTextSize(size);
        t.setPadding(dp(12),dp(8),dp(12),dp(8));
        return t;
    }

    @Override public void onCreate(Bundle b){
        super.onCreate(b);
        root=new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(14),dp(10),dp(14),dp(18));
        root.setBackgroundColor(Color.rgb(7,26,61));

        ScrollView scroll=new ScrollView(this);
        scroll.addView(root);
        setContentView(scroll);

        ImageView logo=new ImageView(this);
        logo.setImageResource(com.betwaz.fxai.R.drawable.betwaz_logo);
        logo.setContentDescription("BETWAZ FX AI logo");
        logo.setScaleType(ImageView.ScaleType.CENTER_INSIDE);
        LinearLayout.LayoutParams logoParams=new LinearLayout.LayoutParams(-1,dp(170));
        logoParams.setMargins(0,dp(4),0,dp(2));
        root.addView(logo,logoParams);

        TextView brand=tv("BETWAZ FX AI",26);
        brand.setTypeface(null,1);
        brand.setGravity(Gravity.CENTER);
        root.addView(brand);

        TextView sub=tv("AI intelligence • decision engine • risk control",11);
        sub.setTextColor(Color.rgb(145,201,237));
        sub.setGravity(Gravity.CENTER);
        root.addView(sub);

        status=tv("SAFE MODE • LIVE TRADING DISABLED",12);
        status.setTextColor(Color.rgb(32,240,181));
        status.setGravity(Gravity.CENTER);
        root.addView(status);

        root.addView(tv("CURRENT MARKET",12));
        symbol=new Spinner(this);
        ArrayAdapter<String> a=new ArrayAdapter<String>(this,android.R.layout.simple_spinner_dropdown_item,symbols);
        symbol.setAdapter(a);
        root.addView(symbol);

        price=tv("Price: loading…",18);
        root.addView(price);

        decision=tv("AI DECISION: WAIT",24);
        decision.setTextColor(Color.YELLOW);
        root.addView(decision);

        scan=new Button(this);
        scan.setText("SCAN MARKET WITH AI");
        scan.setOnClickListener(v->scan());
        root.addView(scan);

        stop=new Button(this);
        stop.setText("EMERGENCY STOP");
        stop.setTextColor(Color.WHITE);
        stop.setBackgroundColor(Color.rgb(165,45,70));
        stop.setOnClickListener(v->emergencyStop());
        root.addView(stop);

        Button refresh=new Button(this);
        refresh.setText("REFRESH MARKET");
        refresh.setOnClickListener(v->refreshMarket());
        root.addView(refresh);

        root.addView(tv("Practice/demo interface only. Live trading remains disabled in the backend.",11));

        symbol.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                refreshMarket();
            }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    void refreshMarket(){
        final String s=(String)symbol.getSelectedItem();
        if(s==null) return;

        price.setText(s+" • Loading market data…");
        decision.setText("AI DECISION: —\nNo scan yet for "+s);
        decision.setTextColor(Color.YELLOW);

        new Thread(()->{
            try{
                JSONObject d=get(API+"/api/market-data?symbol="+URLEncoder.encode(s,"UTF-8"));
                String p=d.optString("price","—");
                boolean open=d.optJSONObject("market_status")!=null && d.optJSONObject("market_status").optBoolean("open");
                runOnUiThread(()->{
                    if(!s.equals(String.valueOf(symbol.getSelectedItem()))) return;
                    price.setText(s+" • Price: "+p+" • "+(open?"MARKET OPEN":"MARKET STATUS UNKNOWN"));
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(!s.equals(String.valueOf(symbol.getSelectedItem()))) return;
                    price.setText(s+" • Market data unavailable\n"+e.getMessage());
                });
            }
        }).start();
    }

    void scan(){
        final String s=(String)symbol.getSelectedItem();
        if(s==null) return;

        scan.setEnabled(false);
        decision.setText("AI DECISION: SCANNING "+s+"…");

        new Thread(()->{
            try{
                JSONObject d=get(API+"/api/ai-test?symbol="+URLEncoder.encode(s,"UTF-8"));
                JSONObject a=d.optJSONObject("analysis");
                if(a==null) a=d;

                String side=a.optString("side","WAIT");
                int q=(int)(Math.max(0,Math.min(1,a.optDouble("confidence",0)))*100);
                String text="AI DECISION ("+s+"): "+side+" • "+q+"%\nEntry: "+a.optString("entry","—")+"  Stop: "+a.optString("stop","—")+"  Target: "+a.optString("target","—");

                runOnUiThread(()->{
                    if(!s.equals(String.valueOf(symbol.getSelectedItem()))) {
                        scan.setEnabled(true);
                        return;
                    }
                    decision.setText(text);
                    decision.setTextColor(side.equals("BUY")?Color.rgb(32,240,181):side.equals("SELL")?Color.rgb(255,79,104):Color.YELLOW);
                    scan.setEnabled(true);
                });
            }catch(Exception e){
                runOnUiThread(()->{
                    if(s.equals(String.valueOf(symbol.getSelectedItem()))) {
                        decision.setText("AI DECISION: WAIT\nScan failed for "+s+"\n"+e.getMessage());
                    }
                    scan.setEnabled(true);
                });
            }
        }).start();
    }

    void emergencyStop(){
        status.setText("SAFE MODE • EMERGENCY STOP REQUESTED");
        Toast.makeText(this,"Backend emergency-stop requires operator authorization.",Toast.LENGTH_LONG).show();
    }

    JSONObject get(String url) throws Exception {
        HttpURLConnection c=(HttpURLConnection)new URL(url).openConnection();
        c.setConnectTimeout(15000);
        c.setReadTimeout(20000);
        c.setRequestMethod("GET");

        int code=c.getResponseCode();
        InputStream stream=(code>=200 && code<300)?c.getInputStream():c.getErrorStream();
        BufferedReader r=new BufferedReader(new InputStreamReader(stream));
        StringBuilder sb=new StringBuilder();
        String line;
        while((line=r.readLine())!=null) sb.append(line);
        r.close();

        if(code<200 || code>=300) throw new IOException("HTTP "+code+": "+sb.toString());
        return new JSONObject(sb.toString());
    }
}
