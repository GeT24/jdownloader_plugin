package jd.plugins.hoster;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Locale;

import org.appwork.utils.formatter.TimeFormatter;
import org.jdownloader.plugins.controller.host.LazyHostPlugin.FEATURE;
import org.jdownloader.scripting.JavaScriptEngineFactory;

import jd.PluginWrapper;
import jd.http.Browser;
import jd.nutils.JDHash;
import jd.nutils.encoding.Encoding;
import jd.plugins.Account;
import jd.plugins.Account.AccountType;
import jd.plugins.AccountInfo;
import jd.plugins.DownloadLink;
import jd.plugins.DownloadLink.AvailableStatus;
import jd.plugins.HostPlugin;
import jd.plugins.LinkStatus;
import jd.plugins.PluginException;
import jd.plugins.PluginForHost;
import jd.plugins.components.PluginJSonUtils;

@HostPlugin(revision = "$Revision: 41665 $", interfaceVersion = 3, names = { "get24.org" }, urls = { "REGEX_NOT_POSSIBLE_RANDOM-asdfasdfsadfsdgfd32423" })
public class GeT24Org extends PluginForHost {
    private static HashMap<Account, HashMap<String, Long>> hostUnavailableMap = new HashMap<Account, HashMap<String, Long>>();
    private static final String                            NICE_HOST          = "get24.org";
    private static final String                            NICE_HOSTproperty  = "get24org";
    private static final String                            VERSION            = "0.0.1";
    private static final Integer                           MAXSIM             = 3;

    public GeT24Org(PluginWrapper wrapper) {
        super(wrapper);
        this.enablePremium("https://get24.org/pricing");
    }

    @Override
    public String getAGBLink() {
        return "https://get24.org/terms";
    }

    @Override
    public FEATURE[] getFeatures() {
        return new FEATURE[] { FEATURE.MULTIHOST };
    }

    private Browser newBrowser() {
        br = new Browser();
        br.setCookiesExclusive(true);
        br.getHeaders().put("User-Agent", "Jdownloader " + VERSION);
        return br;
    }

    @Override
    public AccountInfo fetchAccountInfo(Account account) throws Exception {
        // TODO: status
        final AccountInfo acc_info = new AccountInfo();
        this.br = newBrowser();
        String response = br.postPage("http://dev.get24.org/api/login", "email=" + Encoding.urlEncode(account.getUser()) + "&passwd_sha256=" + JDHash.getSHA256(account.getPass()));
        Long date_expire = TimeFormatter.getMilliSeconds(PluginJSonUtils.getJson(response, "date_expire"), "yyyy-MM-dd", Locale.ENGLISH);
        acc_info.setValidUntil(date_expire);
        acc_info.setTrafficLeft(PluginJSonUtils.getJson(response, "transfer_left"));
        acc_info.setTrafficMax(PluginJSonUtils.getJson(response, "transfer_max"));
        account.setMaxSimultanDownloads(MAXSIM);
        account.setConcurrentUsePossible(true);
        // hosts list
        response = br.getPage("http://dev.get24.org/api/hosts/enabled");
        ArrayList<String> supportedHosts = (ArrayList) JavaScriptEngineFactory.jsonToJavaObject(response);
        acc_info.setMultiHostSupport(this, supportedHosts);
        account.setType(AccountType.PREMIUM);
        // acc_info.setStatus("Premium User");
        return acc_info;
    }

    @Override
    public int getMaxSimultanFreeDownloadNum() {
        return 1;
    }

    public void handleMultiHost(final DownloadLink link, final Account account) throws Exception {
        this.br = newBrowser();
        String post_data = "email=" + Encoding.urlEncode(account.getUser()) + "&passwd_sha256=" + JDHash.getSHA256(account.getPass()) + "&url=" + Encoding.urlEncode(link.getDownloadURL());
        dl = jd.plugins.BrowserAdapter.openDownload(this.br, link, "http://dev.get24.org/api/download", post_data, false, 0);
        if (dl.getConnection().getContentType().equalsIgnoreCase("application/json")) {
            br.followConnection();
            String response = br.toString();
            if (PluginJSonUtils.getJson(response, "reason") == "no transfer") {
                // TODO: wait to midnight 00:00 UTC
                throw new PluginException(LinkStatus.ERROR_PREMIUM, PluginException.VALUE_ID_PREMIUM_TEMP_DISABLE);
            }
            // TODO: invalid credentials etc.
        }
        dl.startDownload();
    }

    @Override
    public AvailableStatus requestFileInformation(DownloadLink parameter) throws Exception {
        return AvailableStatus.UNCHECKABLE;
    }

    @Override
    public void handleFree(DownloadLink link) throws Exception {
    }

    @Override
    public void reset() {
    }

    @Override
    public void resetDownloadlink(DownloadLink link) {
    }
}
