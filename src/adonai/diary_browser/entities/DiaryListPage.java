package adonai.diary_browser.entities;

import java.util.ArrayList;
import android.text.Spanned;


public class DiaryListPage extends ArrayList<ListPage>
{
    private static final long serialVersionUID = 1L;
    private Spanned pageLinks = null;
    private String pageURL = null;

    public Spanned getPageLinks()
    {
        return pageLinks;
    }
    public void setPageLinks(Spanned pageLinks)
    {
        this.pageLinks = pageLinks;
    }
    public String getURL()
    {
        return pageURL;
    }
    public void setURL(String _URL)
    {
        this.pageURL = _URL;
    }
}
