package com.ing.engine.commands.ING;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.execution.exception.ActionException;
import com.ing.engine.execution.exception.ForcedException;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.microsoft.playwright.PlaywrightException;


import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import com.microsoft.playwright.options.Cookie;



public class CommonLogins extends General {

    public CommonLogins(CommandControl cc) {
        super(cc);
    }


    @Action(object = ObjectType.BROWSER, desc = "Iris Login", input = InputType.NO)
        public void irisTokenizedLogin() {
        try {

            String ck = getVar("%ck%");
            String issuer = getVar("%issuer%");
            String type = getVar("%type%");
            int loa = Integer.parseInt(getVar("%loa%"));
            String domain = "." + issuer.substring(issuer.indexOf('.') + 1);

            setCookies(domain);
            injectLocalStorage(ck, issuer, type, loa);


        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error in logging in to Iris :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.GENERAL, desc = "System Time/1000", input = InputType.NO, condition = InputType.YES)
    public void storeCurrentTimeinMillisby1000() {
        try {
            String currentTimeSeconds = String.valueOf(System.currentTimeMillis() / 1000);
            String variableName = Condition;

            if (variableName.matches("%.*%")) {
                addVar(variableName, currentTimeSeconds);
                Report.updateTestLog(Action, "[" + currentTimeSeconds + "]" + " value stored", Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error Storing Time/1000 :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    private void setCookies(String domain) {
        List<Cookie> cookies = new ArrayList<>();
        cookies.add(new Cookie("spin", "dummydummydummydummy")
                .setDomain(domain)
                .setPath("/"));
        cookies.add(new Cookie("lang", "en-GB")
                .setDomain(domain)
                .setPath("/"));


        String setCookieHeader = String.join("\n", response.get(key).headers().allValues("set-cookie"));

        String[] setCookies = setCookieHeader.split("\n");
        for (String setCookie : setCookies) {
            String[] parts = setCookie.split(";");
            String[] nv = parts[0].split("=", 2);
            if (nv.length == 2) {
                cookies.add(new Cookie(nv[0], nv[1])
                        .setDomain(domain)
                        .setPath("/"));
            }
        }

        BrowserContext.addCookies(cookies);
    }

    private void injectLocalStorage(String ck, String issuer, String type, int loa) throws PlaywrightException, ForcedException, ActionException {

        {
            JsonArray data = JsonParser.parseString((String) response.get(key).body()).getAsJsonArray();
            JsonObject tokenData = data.get(0).getAsJsonObject();

            JsonObject token = new JsonObject();
            token.addProperty("accessTokenTimeToLive", 245);
            token.addProperty("default", true);
            token.addProperty("executorLevelOfAssurance", loa);
            token.addProperty("identifyeeType", type);
            token.addProperty("personId", ck);
            token.addProperty("profileId", ck);
            token.addProperty("profileName", "Primary");
            token.addProperty("setId", "testSession");
            token.addProperty("accessToken", tokenData.get("access_token").getAsString().replace("\n", ""));
            token.add("jwtId", tokenData.get("jwt_id"));
            token.add("refreshToken", tokenData.get("refresh_token"));
            token.add("tokenIdentifier", tokenData.get("token_identifier"));
            token.add("unencryptedAccessToken", tokenData.get("unencrypted_access_token"));

            String tokenJson = token.toString();
            String script = String.format("localStorage.setItem('TokenManager.profiles', JSON.stringify([%s]));"
                            + "localStorage.setItem('TokenManager.activeProfile', JSON.stringify('%s'));"
                            + "localStorage.setItem('token-manager.token-%s', JSON.stringify(%s));"
                            + "localStorage.setItem('token-manager.ids', JSON.stringify(['%s']));"
                            + "sessionStorage.setItem('ingAppOcAssistedSessionInitialized', true);",
                    tokenJson, ck, ck, tokenJson, ck);

            BrowserContext.addInitScript(script);
        }

   }

    @Action(object = ObjectType.WEBSERVICE, desc = "Store MING API cookie", condition = InputType.YES)
    public void storeMINGcookies() {
        try {
            Optional<String> cookieHeader = response.get(key).headers().firstValue("set-cookie");
            String cookies = cookieHeader.map(cookie -> cookie.split(";")[0]).orElse("");
            String finalCookies = "spin=secure_random_value; " + cookies + ";";
            if (Condition.matches("%.*%")) {
                addVar(Condition, finalCookies);
                Report.updateTestLog(Action, "Stored MING API cookie: " + cookies, Status.DONE);
            } else {
                Report.updateTestLog(Action, "Variable format is not correct", Status.DEBUG);
            }
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.OFF, null, ex);
            Report.updateTestLog(Action, "Error storing MING API cookie :" + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


}
