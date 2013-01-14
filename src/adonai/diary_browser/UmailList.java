package adonai.diary_browser;

import java.io.IOException;
import java.util.Map;

import org.apache.http.HttpResponse;
import org.apache.http.ParseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.util.EntityUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import com.handmark.pulltorefresh.library.PullToRefreshListView;

import adonai.diary_browser.entities.Openable;
import adonai.diary_browser.entities.DiaryListPage;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.text.Html;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TabWidget;
import android.widget.TextView;

public class UmailList extends Activity implements IRequestHandler, OnClickListener
{
    public void handleBackground(int opCode, Object body)
    {
        pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, true);
        mHandler.sendMessage(mHandler.obtainMessage(opCode, body));
    }

    public void handleUi(int opCode, Object body)
    {
        mUiHandler.sendMessage(mUiHandler.obtainMessage(opCode, body));
    }
    
	static final int TAB_INCOMING                                   =   0;
	static final int TAB_OUTCOMING                                  =   1;
	
	static final int HANDLE_INCOME                                  =   0;
    static final int HANDLE_OUTCOME                                 =   1;
    static final int HANDLE_PROGRESS                                =   7;
    static final int HANDLE_PROGRESS_2                              =   8;
    static final int HANDLE_CONNECTIVITY_ERROR                      = -20;
	
    DiaryHttpClient mDHCL = Globals.mDHCL;
    UserData mUser = Globals.mUser;
    Map<String, String> namesUrls;
    
    DiaryWebView mMessageBrowser;
    PullToRefreshListView mFolderBrowser;
    ProgressDialog pd;
    TabWidget mTabs;
    TextView mIncoming;
    TextView mOutcoming;
    
    Handler mHandler, mUiHandler;
    Looper mLooper; // петля времени

    @Override
    public void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        HandlerThread thr = new HandlerThread("ServiceThread");
        thr.start();
        mLooper = thr.getLooper();
        mHandler = new Handler(mLooper, WorkerCallback);
        mUiHandler = new Handler(UiCallback);
        
        setContentView(R.layout.umail_list_a);
        mMessageBrowser = (DiaryWebView) findViewById(R.id.umessage_browser);
        mMessageBrowser.setDefaultSettings();
        mFolderBrowser = (PullToRefreshListView) findViewById(R.id.ufolder_browser);
        mTabs = (TabWidget) findViewById(R.id.folder_selector);
        
        mIncoming = (TextView) findViewById(R.id.incoming);
        mOutcoming = (TextView) findViewById(R.id.outcoming);
        mIncoming.setOnClickListener(this);
        mOutcoming.setOnClickListener(this);
    }

    @Override
    protected void onStart()
    {
        super.onStart();
        
        pd = ProgressDialog.show(this, getString(R.string.loading), getString(R.string.loading_data), true, true);
        mHandler.sendMessage(mHandler.obtainMessage(HANDLE_INCOME, "http://www.diary.ru/u-mail/folder/?f_id=1"));
    }

    @Override
    public void onBackPressed()
    {
        super.onBackPressed();
        Intent returnIntent = new Intent(getApplicationContext(), DiaryList.class);
        returnIntent.setFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
        startActivity(returnIntent);
        finish();
    }
    
    Handler.Callback UiCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            switch (message.what)
            {
                case HANDLE_PROGRESS:
                    if(pd != null && pd.isShowing())
                        pd.setMessage(getString(R.string.parsing_data));
                return true;
            }
            
            return false;
        }
    };
    
    
    Handler.Callback WorkerCallback = new Handler.Callback()
    {
        public boolean handleMessage(Message message)
        {
            
            try
            {
                switch(message.what)
                {
                    case HANDLE_INCOME:
                    {
                        mDHCL.postPage((String)message.obj, null);
                        HttpResponse page = mDHCL.postPage("http://www.diary.ru/login.php", null);
                        if(page == null)
                        {
                            mUiHandler.sendEmptyMessage(HANDLE_CONNECTIVITY_ERROR);
                            return false;
                        }
                        String incFolder = EntityUtils.toString(page.getEntity());
                        serializeUmailPage(incFolder);
                        
                        mUiHandler.sendEmptyMessage(HANDLE_INCOME);
                    }
                    break;
                }
            }
            catch (ParseException e)
            {
                e.printStackTrace();
            }
            catch (IOException e)
            {
                e.printStackTrace();
            }
            return false;
        }
    };

	public void onClick(View v)
	{
		switch (v.getId())
		{
		case R.id.incoming:
			mTabs.setCurrentTab(TAB_INCOMING);
			break;
		case R.id.outcoming:
			mTabs.setCurrentTab(TAB_OUTCOMING);
			break;
		}
	}
	

    void serializeUmailPage(String dataPage)
    {
        mUser.currentDiaries = new DiaryListPage();
        mUser.currentDiaries.set_URL(Globals.currentURL);
        
        mUiHandler.sendEmptyMessage(HANDLE_PROGRESS);
        Document rootNode = Jsoup.parse(dataPage);
            
        Element table = rootNode.getElementsByAttributeValue("class", "table l").first();
        if(table == null) // Нет вообще никаких сообщений, заканчиваем
            return;
        
        Element pages = rootNode.select("table.pages").first();
        if(pages != null)
            mUser.currentDiaries.setPageLinks(Html.fromHtml(pages.outerHtml()));
        
        Elements rows = table.getElementsByTag("td");
        Element title = null, author = null, last_post = null;
        for (int i = 0; i < rows.size(); ++i)
        {
            if (title == null && rows.get(i).hasClass("l"))
                title = rows.get(i).getElementsByClass("withfloat").first();

            if (author == null)
                author = rows.get(i).getElementsByAttributeValue("target", "_blank").first();
            
            if (last_post == null)
                if (rows.get(i).className().equals(""))
                    last_post = rows.get(i).getElementsByClass("withfloat").first();
            
            if (title != null && author != null && last_post != null)
            {
                Openable diary = new Openable();
                diary.set_title(title.getElementsByTag("b").text());
                diary.set_URL(title.attr("href"));
                
                diary.set_author(author.text());
                String authorData = author.attr("href");
                diary.set_author_URL(authorData);
                diary.set_ID(authorData.substring(authorData.lastIndexOf("?") + 1));
                
                diary.set_last_post(last_post.text());
                diary.set_last_post_URL(last_post.attr("href"));
                
                mUser.currentDiaries.add(diary);
                title = author = last_post = null;
            }
        }
    }
}
