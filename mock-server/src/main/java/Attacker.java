import model.OutInRule;
import model.Request;
import model.Rule;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Attacker {
    private List<Rule> rules;

    public Attacker(List<Rule> rules) {
        this.rules = rules;
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

    public List<String> getIpAddresses() {
        List<String> ipAddresses = new ArrayList<>();
        for (Rule rule : rules) {
            if (rule.getRequest().getPath().matches("(([0-9]{1,3}.){3}[0-9]{1,3})")) {
                Pattern pattern = Pattern.compile("(([0-9]{1,3}.){3}[0-9]{1,3})");
                Matcher matcher = pattern.matcher(rule.getRequest().getPath());
                ipAddresses.add(matcher.group(1));
            }
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
            OutInRule httpFloodRule = new OutInRule(new Request("POST", ipAddress, null, content),
                    null, (long) 0, 2000, (long) 0);
            OutputRequest httpFloodRequest = new OutputRequest(httpFloodRule);
            httpFloodRequest.start();
        }
    }

    //----- ROBUSTNESS ATTACKS -----

    public void verbNotExist() {
        for (String ipAddress : getIpAddresses()) {
            OutInRule wrongVerbRule = new OutInRule(new Request("wrongVerb", ipAddress, null, null),
                    null, (long) 0, 1, null);
            OutputRequest wrongVerbRequest = new OutputRequest(wrongVerbRule);
            wrongVerbRequest.start();
        }
    }

    public void emptyVerb() {
        for (String ipAddress : getIpAddresses()) {
            OutInRule emptyVerbRule = new OutInRule(new Request("", ipAddress, null, null), null,
                    (long) 0, 1, null);
            OutputRequest emptyVerbRequest = new OutputRequest(emptyVerbRule);
            emptyVerbRequest.start();
        }
    }

    public void specialChar() {
        Map<String, String> headers = null;
        headers.put("~`^@=+*", "$€£¤ø");
        for (String ipAddress : getIpAddresses()) {
            OutInRule specialCharRule = new OutInRule(new Request("&'ƒ#%Šµ", ipAddress, headers, null), null,
                    (long) 0, 1, null);
            OutputRequest specialCharRequest = new OutputRequest(specialCharRule);
            specialCharRequest.start();
        }
    }
}
