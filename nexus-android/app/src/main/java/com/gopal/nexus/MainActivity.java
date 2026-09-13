package com.gopal.nexus;

import android.app.*;
import android.os.Bundle;
import android.content.*;
import android.net.Uri;
import android.provider.AlarmClock;
import android.speech.RecognizerIntent;
import android.speech.tts.TextToSpeech;
import android.view.*;
import android.webkit.*;
import android.widget.*;
import androidx.webkit.WebViewAssetLoader;
import org.json.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class MainActivity extends Activity {
    private WebView web;
    private ValueCallback<Uri[]> fileCallback;
    private String backup;
    private TextToSpeech speech;
    private boolean speechReady;
    private static final int EXPORT=10, FILE=11, VOICE=12;
    private static final String START="https://appassets.androidplatform.net/assets/index.html";

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        LinearLayout root=new LinearLayout(this); root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xff0a0a0f);
        root.setOnApplyWindowInsetsListener((v,insets)->{
            v.setPadding(insets.getSystemWindowInsetLeft(),insets.getSystemWindowInsetTop(),insets.getSystemWindowInsetRight(),insets.getSystemWindowInsetBottom());
            return insets.consumeSystemWindowInsets();
        });
        LinearLayout bar=new LinearLayout(this);
        Button work=new Button(this);work.setText("Work");work.setOnClickListener(v->js("go(document.querySelector('[data-s=work]'))"));
        Button gemini=new Button(this);gemini.setText("Open Gemini");gemini.setTextSize(12);gemini.setAllCaps(false);gemini.setOnClickListener(v->openGemini());
        Button menu=new Button(this);menu.setText("App tools");menu.setOnClickListener(v->showTools());
        bar.addView(work,new LinearLayout.LayoutParams(0,48*getResources().getDisplayMetrics().densityDpi/160,1));
        bar.addView(gemini,new LinearLayout.LayoutParams(0,48*getResources().getDisplayMetrics().densityDpi/160,1));
        bar.addView(menu,new LinearLayout.LayoutParams(0,48*getResources().getDisplayMetrics().densityDpi/160,1));root.addView(bar);
        web=new WebView(this);root.addView(web,new LinearLayout.LayoutParams(-1,0,1));setContentView(root);
        WebSettings settings=web.getSettings();settings.setJavaScriptEnabled(true);settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(false);settings.setAllowContentAccess(false);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
        WebViewAssetLoader loader=new WebViewAssetLoader.Builder().addPathHandler("/assets/",new WebViewAssetLoader.AssetsPathHandler(this)).build();
        web.setWebViewClient(new WebViewClient(){
            @Override public WebResourceResponse shouldInterceptRequest(WebView view,WebResourceRequest request){return loader.shouldInterceptRequest(request.getUrl());}
            @Override public boolean shouldOverrideUrlLoading(WebView view,WebResourceRequest request){
                Uri uri=request.getUrl();
                if("https".equals(uri.getScheme())&&"appassets.androidplatform.net".equals(uri.getHost())&&"/assets/index.html".equals(uri.getPath()))return false;
                if(request.isForMainFrame()&&"nexus".equals(uri.getScheme())&&"gemini".equals(uri.getHost())){openGemini();return true;}
                if("nexus".equals(uri.getScheme())&&"backup".equals(uri.getHost())){exportBackup();return true;}
                if("https".equals(uri.getScheme())||"http".equals(uri.getScheme())||"mailto".equals(uri.getScheme())){
                    try{startActivity(new Intent(Intent.ACTION_VIEW,uri));}catch(ActivityNotFoundException e){toast("No app can open this link.");}
                }
                return true;
            }
            @Override public void onPageFinished(WebView view,String url){
                if(START.equals(url))js("window.exportData=function(){location.href='nexus://backup';};");
            }
        });
        web.setWebChromeClient(new WebChromeClient(){
            @Override public boolean onJsAlert(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("OK",(d,w)->result.confirm()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onJsConfirm(WebView v,String url,String message,JsResult result){new AlertDialog.Builder(MainActivity.this).setMessage(message).setPositiveButton("OK",(d,w)->result.confirm()).setNegativeButton("Cancel",(d,w)->result.cancel()).setOnCancelListener(d->result.cancel()).show();return true;}
            @Override public boolean onShowFileChooser(WebView v,ValueCallback<Uri[]> callback,FileChooserParams params){
                if(fileCallback!=null)fileCallback.onReceiveValue(null);fileCallback=callback;
                Intent i=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE);
                try{startActivityForResult(i,FILE);}catch(ActivityNotFoundException e){fileCallback.onReceiveValue(null);fileCallback=null;toast("No file picker available.");}return true;
            }
        });
        speech=new TextToSpeech(this,status->{speechReady=status==TextToSpeech.SUCCESS;});
        web.loadUrl(START);
    }
    private void js(String code){web.evaluateJavascript(code,null);}
    private void toast(String text){Toast.makeText(this,text,Toast.LENGTH_LONG).show();}
    private void openGemini(){
        try {
            Intent launch=getPackageManager().getLaunchIntentForPackage("com.google.android.apps.bard");
            if(launch!=null){startActivity(launch);return;}
        } catch(ActivityNotFoundException | SecurityException ignored) {}
        try {startActivity(new Intent(Intent.ACTION_VIEW,Uri.parse("https://gemini.google.com/")));}
        catch(ActivityNotFoundException | SecurityException e){toast("Install Gemini or enable a browser to continue.");}
    }
    private void showTools(){new AlertDialog.Builder(this).setTitle("NEXUS tools").setItems(new String[]{"Export backup","Import backup","Set a phone alarm","Voice input","Read latest JARVIS reply","Stop speech"},(d,n)->{
        switch(n){
            case 0:exportBackup();break;
            case 1:js("(()=>{const i=document.createElement('input');i.type='file';i.accept='.json,application/json';i.onchange=importData;i.click();})()");break;
            case 2:chooseAlarm();break;
            case 3:voiceInput();break;
            case 4:web.evaluateJavascript("JSON.stringify([...document.querySelectorAll('.msg.ai .msg-bub')].pop()?.textContent||'No reply yet.')",value->{try{String text=new JSONArray("["+value+"]").getString(0);text=new JSONArray("["+text+"]").getString(0);if(speechReady)speech.speak(text,TextToSpeech.QUEUE_FLUSH,null,"nexus");else toast("Speech is not ready.");}catch(Exception e){toast("Unable to read reply.");}});break;
            case 5:if(speech!=null)speech.stop();break;
        }
    }).show();}
    private void exportBackup(){web.evaluateJavascript("JSON.stringify(Object.fromEntries(Object.keys(localStorage).filter(k=>k.startsWith('nx_')).map(k=>[k.slice(3),JSON.parse(localStorage.getItem(k))])))",value->{
        try{backup=new JSONArray("["+value+"]").getString(0);new JSONObject(backup);
            Intent i=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/json").addCategory(Intent.CATEGORY_OPENABLE).putExtra(Intent.EXTRA_TITLE,"nexus-backup.json");startActivityForResult(i,EXPORT);
        }catch(Exception e){toast("Could not prepare backup.");}
    });}
    private void chooseAlarm(){
        new TimePickerDialog(this,(picker,h,m)->{
            EditText label=new EditText(this);label.setHint("Alarm label");
            new AlertDialog.Builder(this).setTitle("Phone alarm").setMessage("Save in your Clock app so it can ring when NEXUS is closed. Manage or cancel it in Clock.").setView(label).setPositiveButton("Open Clock",(d,n)->{
                Intent i=new Intent(AlarmClock.ACTION_SET_ALARM).putExtra(AlarmClock.EXTRA_HOUR,h).putExtra(AlarmClock.EXTRA_MINUTES,m).putExtra(AlarmClock.EXTRA_MESSAGE,label.getText().toString()).putExtra(AlarmClock.EXTRA_SKIP_UI,false);
                try{startActivity(i);}catch(ActivityNotFoundException e){toast("No compatible Clock app installed.");}
            }).setNegativeButton("Cancel",null).show();
        },Calendar.getInstance().get(Calendar.HOUR_OF_DAY),Calendar.getInstance().get(Calendar.MINUTE),true).show();
    }
    private void voiceInput(){Intent i=new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,RecognizerIntent.LANGUAGE_MODEL_FREE_FORM).putExtra(RecognizerIntent.EXTRA_PROMPT,"Speak to JARVIS");try{startActivityForResult(i,VOICE);}catch(ActivityNotFoundException e){toast("Install or enable a speech recognition app.");}}
    @Override protected void onActivityResult(int request,int result,Intent data){super.onActivityResult(request,result,data);
        if(request==FILE){if(fileCallback!=null){fileCallback.onReceiveValue(result==RESULT_OK&&data!=null&&data.getData()!=null?new Uri[]{data.getData()}:null);fileCallback=null;}return;}
        if(result!=RESULT_OK||data==null)return;
        if(request==EXPORT&&backup!=null&&data.getData()!=null){try(OutputStream out=getContentResolver().openOutputStream(data.getData())){if(out==null)throw new IOException();out.write(backup.getBytes(StandardCharsets.UTF_8));toast("Backup saved.");}catch(IOException e){toast("Backup could not be saved.");}finally{backup=null;}}
        if(request==VOICE){ArrayList<String> words=data.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);if(words!=null&&!words.isEmpty())js("go(document.querySelector('[data-s=ai]'));document.getElementById('chatIn').value="+JSONObject.quote(words.get(0))+";");}
    }
    @Override public void onBackPressed(){web.evaluateJavascript("(()=>{const modal=document.querySelector('.modal-ov.vis');if(modal){modal.classList.remove('vis');return true;}if(!document.getElementById('s-dashboard').classList.contains('active')){go(document.querySelector('[data-s=dashboard]'));return true;}return false;})()",value->{if(!"true".equals(value))finish();});}
    @Override protected void onPause(){js("saveAll()");if(speech!=null)speech.stop();super.onPause();}
    @Override protected void onDestroy(){if(fileCallback!=null)fileCallback.onReceiveValue(null);if(speech!=null){speech.stop();speech.shutdown();}web.destroy();super.onDestroy();}
}
