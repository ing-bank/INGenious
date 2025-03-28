package com.ing.engine.commands.syntheticData;

import com.github.javafaker.CreditCardType;
import com.github.javafaker.Faker;
import com.github.javafaker.Internet;
import com.ing.engine.commands.browser.Command;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.Action;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

public class SyntheticDataGenerator extends Command {

    public SyntheticDataGenerator(CommandControl cc) {
        super(cc);
        if (faker.get(key) == null) {
            faker.put(key, new Faker(new Locale("en-US")));
        }
    }

    @Action(object = ObjectType.DATA, desc = "Set Faker locale for testing", input = InputType.YES)
    public void setLocale() {
        try {
            String locale = Data;
            Faker fakerWithLocale = new Faker(new Locale(locale));
            faker.put(key, fakerWithLocale);
            Report.updateTestLog(Action, "Faker locale set to " + locale, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during locale setup", ex);
            Report.updateTestLog(Action, "Error setting locale: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random street address", input = InputType.YES, condition = InputType.NO)
    public void streetAddress() {
        try {
            String strObj = Input;
            String streetAddress = faker.get(key).address().streetAddress();
            Report.updateTestLog(Action, "Generated data: " + streetAddress, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, streetAddress);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random street name", input = InputType.YES, condition = InputType.NO)
    public void streetName() {
        try {
            String strObj = Input;
            String streetName = faker.get(key).address().streetName();
            Report.updateTestLog(Action, "Generated data: " + streetName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, streetName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random secondary address", input = InputType.YES, condition = InputType.NO)
    public void secondaryAddress() {
        try {
            String strObj = Input;
            String secondaryAddress = faker.get(key).address().secondaryAddress();
            Report.updateTestLog(Action, "Generated data: " + secondaryAddress, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, secondaryAddress);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random building number", input = InputType.YES, condition = InputType.NO)
    public void buildingNumber() {
        try {
            String strObj = Input;
            String buildingNumber = faker.get(key).address().buildingNumber();
            Report.updateTestLog(Action, "Generated data: " + buildingNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, buildingNumber);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random address city", input = InputType.YES, condition = InputType.NO)
    public void city() {
        try {
            String strObj = Input;
            String city = faker.get(key).address().city();
            Report.updateTestLog(Action, "Generated data: " + city, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, city);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random city prefix", input = InputType.YES, condition = InputType.NO)
    public void cityPrefix() {
        try {
            String strObj = Input;
            String cityPrefix = faker.get(key).address().cityPrefix();
            Report.updateTestLog(Action, "Generated data: " + cityPrefix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, cityPrefix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random city suffix", input = InputType.YES, condition = InputType.NO)
    public void citySuffix() {
        try {
            String strObj = Input;
            String citySuffix = faker.get(key).address().citySuffix();
            Report.updateTestLog(Action, "Generated data: " + citySuffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, citySuffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random zip code", input = InputType.YES, condition = InputType.NO)
    public void zipCode() {
        try {
            String strObj = Input;
            String zipCode = faker.get(key).address().zipCode();
            Report.updateTestLog(Action, "Generated data: " + zipCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, zipCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random zip code by state", input = InputType.YES, condition = InputType.YES)
    public void zipCodeByState() {
        try {
            String strObj = Input;
            String stateAbbreviation = Condition;
            String zipCodeByState = faker.get(key).address().zipCodeByState(stateAbbreviation);
            Report.updateTestLog(Action, "Generated data: " + zipCodeByState, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, zipCodeByState);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random state", input = InputType.YES, condition = InputType.NO)
    public void state() {
        try {
            String strObj = Input;
            String state = faker.get(key).address().state();
            Report.updateTestLog(Action, "Generated data: " + state, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, state);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random state abbreviation", input = InputType.YES, condition = InputType.NO)
    public void stateAbbreviation() {
        try {
            String strObj = Input;
            String stateAbbreviation = faker.get(key).address().stateAbbr();
            Report.updateTestLog(Action, "Generated data: " + stateAbbreviation, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, stateAbbreviation);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random country", input = InputType.YES, condition = InputType.NO)
    public void country() {
        try {
            String strObj = Input;
            String country = faker.get(key).address().country();
            Report.updateTestLog(Action, "Generated data: " + country, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, country);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random country code", input = InputType.YES, condition = InputType.NO)
    public void countryCode() {
        try {
            String strObj = Input;
            String countryCode = faker.get(key).address().countryCode();
            Report.updateTestLog(Action, "Generated data: " + countryCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, countryCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random latitude", input = InputType.YES, condition = InputType.NO)
    public void latitude() {
        try {
            String strObj = Input;
            String latitude = faker.get(key).address().latitude();
            Report.updateTestLog(Action, "Generated data: " + latitude, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, latitude);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random longitude", input = InputType.YES, condition = InputType.NO)
    public void longitude() {
        try {
            String strObj = Input;
            String longitude = faker.get(key).address().longitude();
            Report.updateTestLog(Action, "Generated data: " + longitude, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, longitude);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random full address", input = InputType.YES, condition = InputType.NO)
    public void fullAddress() {
        try {
            String strObj = Input;
            String fullAddress = faker.get(key).address().fullAddress();
            Report.updateTestLog(Action, "Generated data: " + fullAddress, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, fullAddress);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate country by zip code", input = InputType.YES, condition = InputType.YES)
    public void countryByZipCode() {
        try {
            String strObj = Input;
            String zipCode = Condition;
            String countryByZipCode = faker.get(key).address().countyByZipCode(zipCode);
            Report.updateTestLog(Action, "Generated data: " + countryByZipCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, countryByZipCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random street address including secondary", input = InputType.YES, condition = InputType.NO)
    public void streetAddressIncludeSecondary() {
        try {
            String strObj = Input;
            boolean includeSecondary = true;
            String streetAddress = faker.get(key).address().streetAddress(includeSecondary);
            Report.updateTestLog(Action, "Generated data: " + streetAddress, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, streetAddress);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random street address number", input = InputType.YES, condition = InputType.NO)
    public void streetAddressNumber() {
        try {
            String strObj = Input;
            String streetAddressNumber = faker.get(key).address().streetAddressNumber();
            Report.updateTestLog(Action, "Generated data: " + streetAddressNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, streetAddressNumber);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random street prefix", input = InputType.YES, condition = InputType.NO)
    public void streetPrefix() {
        try {
            String strObj = Input;
            String streetPrefix = faker.get(key).address().streetPrefix();
            Report.updateTestLog(Action, "Generated data: " + streetPrefix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, streetPrefix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random street suffix", input = InputType.YES, condition = InputType.NO)
    public void streetSuffix() {
        try {
            String strObj = Input;
            String streetSuffix = faker.get(key).address().streetSuffix();
            Report.updateTestLog(Action, "Generated data: " + streetSuffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, streetSuffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random time zone", input = InputType.YES, condition = InputType.NO)
    public void timezone() {
        try {
            String strObj = Input;
            String timezone = faker.get(key).address().timeZone();
            Report.updateTestLog(Action, "Generated data: " + timezone, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, timezone);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random city Name", input = InputType.YES, condition = InputType.NO)
    public void cityName() {
        try {
            String strObj = Input;
            String cityName = faker.get(key).address().cityName();
            Report.updateTestLog(Action, "Generated data: " + cityName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, cityName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random address first name", input = InputType.YES, condition = InputType.NO)
    public void addressFirstName() {
        try {
            String strObj = Input;
            String firstName = faker.get(key).address().firstName();
            Report.updateTestLog(Action, "Generated data: " + firstName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, firstName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random address last name", input = InputType.YES, condition = InputType.NO)
    public void addressLastName() {
        try {
            String strObj = Input;
            String lastName = faker.get(key).address().lastName();
            Report.updateTestLog(Action, "Generated data: " + lastName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, lastName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random God's name", input = InputType.YES, condition = InputType.NO)
    public void god() {
        try {
            String strObj = Input;
            String god = faker.get(key).ancient().god();
            Report.updateTestLog(Action, "Generated data: " + god, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, god);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a primordial deity's name", input = InputType.YES, condition = InputType.NO)
    public void primordial() {
        try {
            String strObj = Input;
            String primordial = faker.get(key).ancient().primordial();
            Report.updateTestLog(Action, "Generated data: " + primordial, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, primordial);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random titan's name", input = InputType.YES, condition = InputType.NO)
    public void titan() {
        try {
            String strObj = Input;
            String titan = faker.get(key).ancient().titan();
            Report.updateTestLog(Action, "Generated data: " + titan, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, titan);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random hero's name", input = InputType.YES, condition = InputType.NO)
    public void hero() {
        try {
            String strObj = Input;
            String hero = faker.get(key).ancient().hero();
            Report.updateTestLog(Action, "Generated data: " + hero, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hero);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random application name", input = InputType.YES, condition = InputType.NO)
    public void appName() {
        try {
            String strObj = Input;
            String appName = faker.get(key).app().name();
            Report.updateTestLog(Action, "Generated data: " + appName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, appName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random application version", input = InputType.YES, condition = InputType.NO)
    public void appVersion() {
        try {
            String strObj = Input;
            String appVersion = faker.get(key).app().version();
            Report.updateTestLog(Action, "Generated data: " + appVersion, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, appVersion);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random application author", input = InputType.YES, condition = InputType.NO)
    public void appAuthor() {
        try {
            String strObj = Input;
            String appAuthor = faker.get(key).app().author();
            Report.updateTestLog(Action, "Generated data: " + appAuthor, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, appAuthor);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random artist's name", input = InputType.YES, condition = InputType.NO)
    public void artistName() {
        try {
            String strObj = Input;
            String artistName = faker.get(key).artist().name();
            Report.updateTestLog(Action, "Generated data: " + artistName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, artistName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random avatar URL", input = InputType.YES, condition = InputType.NO)
    public void avatarUrl() {
        try {
            String strObj = Input;
            String avatarUrl = faker.get(key).avatar().image();
            Report.updateTestLog(Action, "Generated data: " + avatarUrl, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, avatarUrl);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random aircraft name", input = InputType.YES, condition = InputType.NO)
    public void aircraft() {
        try {
            String strObj = Input;
            String aircraft = faker.get(key).aviation().aircraft();
            Report.updateTestLog(Action, "Generated data: " + aircraft, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, aircraft);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random airport name", input = InputType.YES, condition = InputType.NO)
    public void airport() {
        try {
            String strObj = Input;
            String airport = faker.get(key).aviation().airport();
            Report.updateTestLog(Action, "Generated data: " + airport, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, airport);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random METAR", input = InputType.YES, condition = InputType.NO)
    public void metar() {
        try {
            String strObj = Input;
            String metar = faker.get(key).aviation().METAR();
            Report.updateTestLog(Action, "Generated data: " + metar, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, metar);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a character name from Back to the Future", input = InputType.YES, condition = InputType.NO)
    public void characterBackToTheFuture() {
        try {
            String strObj = Input;
            String character = faker.get(key).backToTheFuture().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a quote from Back to the Future", input = InputType.YES, condition = InputType.NO)
    public void quoteBackToTheFuture() {
        try {
            String strObj = Input;
            String quote = faker.get(key).backToTheFuture().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a date from Back to the Future", input = InputType.YES, condition = InputType.NO)
    public void dateBackToTheFuture() {
        try {
            String strObj = Input;
            String date = faker.get(key).backToTheFuture().date();
            Report.updateTestLog(Action, "Generated data: " + date, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, date);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random beer name", input = InputType.YES, condition = InputType.NO)
    public void beerName() {
        try {
            String strObj = Input;
            String beerName = faker.get(key).beer().name();
            Report.updateTestLog(Action, "Generated data: " + beerName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, beerName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random beer style", input = InputType.YES, condition = InputType.NO)
    public void beerStyle() {
        try {
            String strObj = Input;
            String beerStyle = faker.get(key).beer().style();
            Report.updateTestLog(Action, "Generated data: " + beerStyle, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, beerStyle);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random beer hop variety", input = InputType.YES, condition = InputType.NO)
    public void beerHop() {
        try {
            String strObj = Input;
            String beerHop = faker.get(key).beer().hop();
            Report.updateTestLog(Action, "Generated data: " + beerHop, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, beerHop);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random beer yeast variety", input = InputType.YES, condition = InputType.NO)
    public void beerYeast() {
        try {
            String strObj = Input;
            String beerYeast = faker.get(key).beer().yeast();
            Report.updateTestLog(Action, "Generated data: " + beerYeast, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, beerYeast);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random beer malt variety", input = InputType.YES, condition = InputType.NO)
    public void beerMalt() {
        try {
            String strObj = Input;
            String beerMalt = faker.get(key).beer().malt();
            Report.updateTestLog(Action, "Generated data: " + beerMalt, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, beerMalt);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random book title", input = InputType.YES, condition = InputType.NO)
    public void bookTitle() {
        try {
            String strObj = Input;
            String bookTitle = faker.get(key).book().title();
            Report.updateTestLog(Action, "Generated data: " + bookTitle, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, bookTitle);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random book author", input = InputType.YES, condition = InputType.NO)
    public void bookAuthor() {
        try {
            String strObj = Input;
            String bookAuthor = faker.get(key).book().author();
            Report.updateTestLog(Action, "Generated data: " + bookAuthor, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, bookAuthor);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random book genre", input = InputType.YES, condition = InputType.NO)
    public void bookGenre() {
        try {
            String strObj = Input;
            String bookGenre = faker.get(key).book().genre();
            Report.updateTestLog(Action, "Generated data: " + bookGenre, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, bookGenre);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random book publisher", input = InputType.YES, condition = InputType.NO)
    public void bookPublisher() {
        try {
            String strObj = Input;
            String bookPublisher = faker.get(key).book().publisher();
            Report.updateTestLog(Action, "Generated data: " + bookPublisher, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, bookPublisher);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random boolean value", input = InputType.YES, condition = InputType.NO)
    public void randomBool() {
        try {
            String strObj = Input;
            boolean randomBool = faker.get(key).bool().bool();
            Report.updateTestLog(Action, "Generated data: " + randomBool, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Boolean.toString(randomBool));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random credit card number", input = InputType.YES, condition = InputType.NO)
    public void creditCardNumber() {
        try {
            String strObj = Input;
            String creditCardNumber = faker.get(key).business().creditCardNumber();
            Report.updateTestLog(Action, "Generated data: " + creditCardNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, creditCardNumber);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random credit card type", input = InputType.YES, condition = InputType.NO)
    public void creditCardType() {
        try {
            String strObj = Input;
            String creditCardType = faker.get(key).business().creditCardType();
            Report.updateTestLog(Action, "Generated data: " + creditCardType, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, creditCardType);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random credit card expiration date", input = InputType.YES, condition = InputType.NO)
    public void creditCardExpiry() {
        try {
            String strObj = Input;
            String creditCardExpiry = faker.get(key).business().creditCardExpiry();
            Report.updateTestLog(Action, "Generated data: " + creditCardExpiry, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, creditCardExpiry);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random cat name", input = InputType.YES, condition = InputType.NO)
    public void catName() {
        try {
            String strObj = Input;
            String catName = faker.get(key).cat().name();
            Report.updateTestLog(Action, "Generated data: " + catName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, catName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random cat breed", input = InputType.YES, condition = InputType.NO)
    public void catBreed() {
        try {
            String strObj = Input;
            String catBreed = faker.get(key).cat().breed();
            Report.updateTestLog(Action, "Generated data: " + catBreed, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, catBreed);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random cat registry", input = InputType.YES, condition = InputType.NO)
    public void catRegistry() {
        try {
            String strObj = Input;
            String catRegistry = faker.get(key).cat().registry();
            Report.updateTestLog(Action, "Generated data: " + catRegistry, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, catRegistry);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Chuck Norris fact", input = InputType.YES, condition = InputType.NO)
    public void chuckNorrisFact() {
        try {
            String strObj = Input;
            String chuckNorrisFact = faker.get(key).chuckNorris().fact();
            Report.updateTestLog(Action, "Generated data: " + chuckNorrisFact, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, chuckNorrisFact);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN-10 number", input = InputType.YES, condition = InputType.NO)
    public void isbn10() {
        try {
            String strObj = Input;
            String isbn10 = faker.get(key).code().isbn10();
            Report.updateTestLog(Action, "Generated data: " + isbn10, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbn10);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN-13 number", input = InputType.YES, condition = InputType.NO)
    public void isbn13() {
        try {
            String strObj = Input;
            String isbn13 = faker.get(key).code().isbn13();
            Report.updateTestLog(Action, "Generated data: " + isbn13, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbn13);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random GTIN-8 code", input = InputType.YES, condition = InputType.NO)
    public void gtin8() {
        try {
            String strObj = Input;
            String gtin8 = faker.get(key).code().gtin8();
            Report.updateTestLog(Action, "Generated data: " + gtin8, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, gtin8);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random GTIN-13 code", input = InputType.YES, condition = InputType.NO)
    public void gtin13() {
        try {
            String strObj = Input;
            String gtin13 = faker.get(key).code().gtin13();
            Report.updateTestLog(Action, "Generated data: " + gtin13, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, gtin13);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random EAN-8 code", input = InputType.YES, condition = InputType.NO)
    public void ean8() {
        try {
            String strObj = Input;
            String ean8 = faker.get(key).code().ean8();
            Report.updateTestLog(Action, "Generated data: " + ean8, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ean8);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random EAN-13 code", input = InputType.YES, condition = InputType.NO)
    public void ean13() {
        try {
            String strObj = Input;
            String ean13 = faker.get(key).code().ean13();
            Report.updateTestLog(Action, "Generated data: " + ean13, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ean13);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ASIN number", input = InputType.YES, condition = InputType.NO)
    public void asin() {
        try {
            String strObj = Input;
            String issn = faker.get(key).code().asin();
            Report.updateTestLog(Action, "Generated data: " + issn, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, issn);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IMEI number", input = InputType.YES, condition = InputType.NO)
    public void imei() {
        try {
            String strObj = Input;
            String issn = faker.get(key).code().imei();
            Report.updateTestLog(Action, "Generated data: " + issn, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, issn);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN group", input = InputType.YES, condition = InputType.NO)
    public void isbnGroup() {
        try {
            String strObj = Input;
            String isbnGroup = faker.get(key).code().isbnGroup();
            Report.updateTestLog(Action, "Generated data: " + isbnGroup, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbnGroup);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN GS1 number", input = InputType.YES, condition = InputType.NO)
    public void isbnGs1() {
        try {
            String strObj = Input;
            String isbnGs1 = faker.get(key).code().isbnGs1();
            Report.updateTestLog(Action, "Generated data: " + isbnGs1, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbnGs1);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN registrant number", input = InputType.YES, condition = InputType.NO)
    public void isbnRegistrant() {
        try {
            String strObj = Input;
            String isbnRegistrant = faker.get(key).code().isbnRegistrant();
            Report.updateTestLog(Action, "Generated data: " + isbnRegistrant, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbnRegistrant);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN-10 number with separator", input = InputType.YES, condition = InputType.NO)
    public void isbn10WithSeparator() {
        try {
            String strObj = Input;
            boolean separator = true;
            String isbn10 = faker.get(key).code().isbn10(separator);
            Report.updateTestLog(Action, "Generated data: " + isbn10, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbn10);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ISBN-13 number with separator", input = InputType.YES, condition = InputType.NO)
    public void isbn13WithSeparator() {
        try {
            String strObj = Input;
            boolean separator = true;
            String isbn13 = faker.get(key).code().isbn13(separator);
            Report.updateTestLog(Action, "Generated data: " + isbn13, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, isbn13);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random color name", input = InputType.YES, condition = InputType.NO)
    public void colorName() {
        try {
            String strObj = Input;
            String colorName = faker.get(key).color().name();
            Report.updateTestLog(Action, "Generated data: " + colorName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, colorName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random hexadecimal color code", input = InputType.YES, condition = InputType.NO)
    public void hex() {
        try {
            String strObj = Input;
            String hex = "color" + faker.get(key).color().hex();
            Report.updateTestLog(Action, "Generated data: " + hex, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hex);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random hexadecimal color code (full form)", input = InputType.YES, condition = InputType.NO)
    public void hexIncludeHashSign() {
        try {
            String strObj = Input;
            boolean includeHashSign = true;
            String hex = "color" + faker.get(key).color().hex(includeHashSign);
            Report.updateTestLog(Action, "Generated data: " + hex, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hex);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random product name", input = InputType.YES, condition = InputType.NO)
    public void productName() {
        try {
            String strObj = Input;
            String productName = faker.get(key).commerce().productName();
            Report.updateTestLog(Action, "Generated data: " + productName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, productName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random commerce department name", input = InputType.YES, condition = InputType.NO)
    public void commerceDepartment() {
        try {
            String strObj = Input;
            String department = faker.get(key).commerce().department();
            Report.updateTestLog(Action, "Generated data: " + department, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, department);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random price", input = InputType.YES, condition = InputType.NO)
    public void commercePrice() {
        try {
            String strObj = Input;
            String price = faker.get(key).commerce().price();
            Report.updateTestLog(Action, "Generated data: " + price, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, price);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate price within a range", input = InputType.YES, condition = InputType.YES)
    public void priceWithinRange() {
        try {
            String strObj = Input;
            String inputMin = Condition.split(":", 2)[0];
            String inputMax = Condition.split(":", 2)[1];
            Double min = Double.parseDouble(inputMin);
            Double max = Double.parseDouble(inputMax);
            String price = faker.get(key).commerce().price(min, max);
            Report.updateTestLog(Action, "Generated data: " + price, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, price);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random material name", input = InputType.YES, condition = InputType.NO)
    public void commerceMaterial() {
        try {
            String strObj = Input;
            String material = faker.get(key).commerce().material();
            Report.updateTestLog(Action, "Generated data: " + material, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, material);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random color name", input = InputType.YES, condition = InputType.NO)
    public void commerceColor() {
        try {
            String strObj = Input;
            String color = faker.get(key).commerce().color();
            Report.updateTestLog(Action, "Generated data: " + color, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, color);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random promotion code", input = InputType.YES, condition = InputType.NO)
    public void promotionCode() {
        try {
            String strObj = Input;
            String promotionCode = faker.get(key).commerce().promotionCode();
            Report.updateTestLog(Action, "Generated data: " + promotionCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, promotionCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random promotion code with digits", input = InputType.YES, condition = InputType.YES)
    public void promotionCodeWithDigits() {
        try {
            String strObj = Input;
            String digitStr = Condition;
            Integer digits = Integer.parseInt(digitStr);
            String promotionCode = faker.get(key).commerce().promotionCode(digits);
            Report.updateTestLog(Action, "Generated data: " + promotionCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, promotionCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company name", input = InputType.YES, condition = InputType.NO)
    public void companyName() {
        try {
            String strObj = Input;
            String companyName = faker.get(key).company().name();
            Report.updateTestLog(Action, "Generated data: " + companyName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, companyName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company industry", input = InputType.YES, condition = InputType.NO)
    public void companyIndustry() {
        try {
            String strObj = Input;
            String companyIndustry = faker.get(key).company().industry();
            Report.updateTestLog(Action, "Generated data: " + companyIndustry, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, companyIndustry);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company catchphrase", input = InputType.YES, condition = InputType.NO)
    public void companyCatchPhrase() {
        try {
            String strObj = Input;
            String companyCatchPhrase = faker.get(key).company().catchPhrase();
            Report.updateTestLog(Action, "Generated data: " + companyCatchPhrase, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, companyCatchPhrase);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company buzzword", input = InputType.YES, condition = InputType.NO)
    public void companyBuzzword() {
        try {
            String strObj = Input;
            String companyBuzzword = faker.get(key).company().buzzword();
            Report.updateTestLog(Action, "Generated data: " + companyBuzzword, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, companyBuzzword);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random BS (business speak)", input = InputType.YES, condition = InputType.NO)
    public void companyBS() {
        try {
            String strObj = Input;
            String bs = faker.get(key).company().bs();
            Report.updateTestLog(Action, "Generated data: " + bs, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, bs);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company logo URL", input = InputType.YES, condition = InputType.NO)
    public void companyLogo() {
        try {
            String strObj = Input;
            String logo = faker.get(key).company().logo();
            Report.updateTestLog(Action, "Generated data: " + logo, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, logo);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random profession", input = InputType.YES, condition = InputType.NO)
    public void profession() {
        try {
            String strObj = Input;
            String profession = faker.get(key).company().profession();
            Report.updateTestLog(Action, "Generated data: " + profession, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, profession);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company suffix", input = InputType.YES, condition = InputType.NO)
    public void companySuffix() {
        try {
            String strObj = Input;
            String suffix = faker.get(key).company().suffix();
            Report.updateTestLog(Action, "Generated data: " + suffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, suffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random company URL", input = InputType.YES, condition = InputType.NO)
    public void companyUrl() {
        try {
            String strObj = Input;
            String url = faker.get(key).company().url();
            Report.updateTestLog(Action, "Generated data: " + url, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, url);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random capital city", input = InputType.YES, condition = InputType.NO)
    public void capital() {
        try {
            String strObj = Input;
            String capital = faker.get(key).country().capital();
            Report.updateTestLog(Action, "Generated data: " + capital, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, capital);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random 2-letter country code", input = InputType.YES, condition = InputType.NO)
    public void countryCode2() {
        try {
            String strObj = Input;
            String countryCode2 = faker.get(key).country().countryCode2();
            Report.updateTestLog(Action, "Generated data: " + countryCode2, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, countryCode2);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random 3-letter country code", input = InputType.YES, condition = InputType.NO)
    public void countryCode3() {
        try {
            String strObj = Input;
            String countryCode3 = faker.get(key).country().countryCode3();
            Report.updateTestLog(Action, "Generated data: " + countryCode3, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, countryCode3);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random currency name", input = InputType.YES, condition = InputType.NO)
    public void countryCurrency() {
        try {
            String strObj = Input;
            String currency = faker.get(key).country().currency();
            Report.updateTestLog(Action, "Generated data: " + currency, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, currency);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random currency code", input = InputType.YES, condition = InputType.NO)
    public void countryCurrencyCode() {
        try {
            String strObj = Input;
            String currencyCode = faker.get(key).country().currencyCode();
            Report.updateTestLog(Action, "Generated data: " + currencyCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, currencyCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random country flag", input = InputType.YES, condition = InputType.NO)
    public void flag() {
        try {
            String strObj = Input;
            String flag = faker.get(key).country().flag();
            Report.updateTestLog(Action, "Generated data: " + flag, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, flag);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random country name", input = InputType.YES, condition = InputType.NO)
    public void countryName() {
        try {
            String strObj = Input;
            String name = faker.get(key).country().name();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random MD5 hash", input = InputType.YES, condition = InputType.NO)
    public void md5() {
        try {
            String strObj = Input;
            String md5 = faker.get(key).crypto().md5();
            Report.updateTestLog(Action, "Generated data: " + md5, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, md5);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random SHA-1 hash", input = InputType.YES, condition = InputType.NO)
    public void sha1() {
        try {
            String strObj = Input;
            String sha1 = faker.get(key).crypto().sha1();
            Report.updateTestLog(Action, "Generated data: " + sha1, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sha1);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random SHA-256 hash", input = InputType.YES, condition = InputType.NO)
    public void sha256() {
        try {
            String strObj = Input;
            String sha256 = faker.get(key).crypto().sha256();
            Report.updateTestLog(Action, "Generated data: " + sha256, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sha256);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random SHA-512 hash", input = InputType.YES, condition = InputType.NO)
    public void sha512() {
        try {
            String strObj = Input;
            String sha512 = faker.get(key).crypto().sha512();
            Report.updateTestLog(Action, "Generated data: " + sha512, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sha512);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random currency name", input = InputType.YES, condition = InputType.NO)
    public void currencyName() {
        try {
            String strObj = Input;
            String currency = faker.get(key).currency().name();
            Report.updateTestLog(Action, "Generated data: " + currency, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, currency);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random currency code", input = InputType.YES, condition = InputType.NO)
    public void currencyCode() {
        try {
            String strObj = Input;
            String currencyCode = faker.get(key).currency().code();
            Report.updateTestLog(Action, "Generated data: " + currencyCode, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, currencyCode);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a future date from now", input = InputType.YES, condition = InputType.YES)
    public void futureUpto() {
        try {
            String strObj = Input;
            String max = Condition.split(":", 2)[0];
            int atMost = Integer.parseInt(max);
            String unitStr = Condition.split(":", 2)[1];
            TimeUnit unit = TimeUnit.valueOf(unitStr);
            Date futureDate = faker.get(key).date().future(atMost, unit);
            Report.updateTestLog(Action, "Generated data: " + futureDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, futureDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a future date from now, with a minimum time", input = InputType.YES, condition = InputType.YES)
    public void futureWithinRange() {
        try {
            String strObj = Input;
            String max = Condition.split(":", 3)[0];
            String min = Condition.split(":", 3)[1];
            int atMost = Integer.parseInt(max);
            int minimum = Integer.parseInt(min);
            String unitStr = Condition.split(":", 3)[2];
            TimeUnit unit = TimeUnit.valueOf(unitStr);
            Date futureDate = faker.get(key).date().future(atMost, minimum, unit);
            Report.updateTestLog(Action, "Generated data: " + futureDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, futureDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a future date relative to the reference date", input = InputType.YES, condition = InputType.YES)
    public void futureUptoBasedOnRefDate() {
        try {
            String strObj = Input;
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String max = Condition.split(":", 3)[0];
            int atMost = Integer.parseInt(max);
            String unitStr = Condition.split(":", 3)[1];
            TimeUnit unit = TimeUnit.valueOf(unitStr);
            String dateStr = Condition.split(":", 3)[2];
            Date referenceDate = formatter.parse(dateStr);
            Date futureDate = faker.get(key).date().future(atMost, unit, referenceDate);
            Report.updateTestLog(Action, "Generated data: " + futureDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, futureDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a past date from now", input = InputType.YES, condition = InputType.YES)
    public void pastUpto() {
        try {
            String strObj = Input;
            String max = Condition.split(":", 2)[0];
            int atMost = Integer.parseInt(max);
            String unitStr = Condition.split(":", 2)[1];
            TimeUnit unit = TimeUnit.valueOf(unitStr);
            Date pastDate = faker.get(key).date().past(atMost, unit);
            Report.updateTestLog(Action, "Generated data: " + pastDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, pastDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a past date from now, with a minimum time", input = InputType.YES, condition = InputType.YES)
    public void pastWithinRange() {
        try {
            String strObj = Input;
            String max = Condition.split(":", 3)[0];
            String min = Condition.split(":", 3)[1];
            int atMost = Integer.parseInt(max);
            int minimum = Integer.parseInt(min);
            String unitStr = Condition.split(":", 3)[2];
            TimeUnit unit = TimeUnit.valueOf(unitStr);
            Date pastDate = faker.get(key).date().past(atMost, minimum, unit);
            Report.updateTestLog(Action, "Generated data: " + pastDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, pastDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a past date relative to the reference date", input = InputType.YES, condition = InputType.YES)
    public void pastUptoBasedOnRefDate() {
        try {
            String strObj = Input;
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String max = Condition.split(":", 3)[0];
            int atMost = Integer.parseInt(max);
            String unitStr = Condition.split(":", 3)[1];
            TimeUnit unit = TimeUnit.valueOf(unitStr);
            String dateStr = Condition.split(":", 3)[2];
            Date referenceDate = formatter.parse(dateStr);
            Date pastDate = faker.get(key).date().past(atMost, unit, referenceDate);
            Report.updateTestLog(Action, "Generated data: " + pastDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, pastDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random date between two dates", input = InputType.YES, condition = InputType.YES)
    public void dateBetween() {
        try {
            String strObj = Input;
            SimpleDateFormat formatter = new SimpleDateFormat("dd/MM/yyyy");
            String from = Condition.split(":", 2)[0];
            String to = Condition.split(":", 2)[1];
            Date startDate = formatter.parse(from);
            Date endDate = formatter.parse(to);
            Date betweenDate = faker.get(key).date().between(startDate, endDate);
            Report.updateTestLog(Action, "Generated data: " + betweenDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, betweenDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a random birthday between 65 and 18 years ago", input = InputType.YES, condition = InputType.NO)
    public void birthday() {
        try {
            String strObj = Input;
            Date birthdayDate = faker.get(key).date().birthday();
            Report.updateTestLog(Action, "Generated data: " + birthdayDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, birthdayDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generates a random birthday between two ages", input = InputType.YES, condition = InputType.YES)
    public void birthdayWithinRange() {
        try {
            String strObj = Input;
            String from = Condition.split(":", 2)[0];
            String to = Condition.split(":", 2)[1];
            int minAge = Integer.parseInt(from);
            int maxAge = Integer.parseInt(to);
            Date birthdayDate = faker.get(key).date().birthday(minAge, maxAge);
            Report.updateTestLog(Action, "Generated data: " + birthdayDate, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, birthdayDate.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random race", input = InputType.YES, condition = InputType.NO)
    public void race() {
        try {
            String strObj = Input;
            String race = faker.get(key).demographic().race();
            Report.updateTestLog(Action, "Generated data: " + race, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, race);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random educational attainment", input = InputType.YES, condition = InputType.NO)
    public void educationalAttainment() {
        try {
            String strObj = Input;
            String educationalAttainment = faker.get(key).demographic().educationalAttainment();
            Report.updateTestLog(Action, "Generated data: " + educationalAttainment, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, educationalAttainment);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random demographic sex", input = InputType.YES, condition = InputType.NO)
    public void sex() {
        try {
            String strObj = Input;
            String sex = faker.get(key).demographic().sex();
            Report.updateTestLog(Action, "Generated data: " + sex, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sex);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random marital status", input = InputType.YES, condition = InputType.NO)
    public void maritalStatus() {
        try {
            String strObj = Input;
            String maritalStatus = faker.get(key).demographic().maritalStatus();
            Report.updateTestLog(Action, "Generated data: " + maritalStatus, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, maritalStatus);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random demographic D.P.", input = InputType.YES, condition = InputType.NO)
    public void demonym() {
        try {
            String strObj = Input;
            String demonym = faker.get(key).demographic().demonym();
            Report.updateTestLog(Action, "Generated data: " + demonym, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, demonym);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog name", input = InputType.YES, condition = InputType.NO)
    public void dogName() {
        try {
            String strObj = Input;
            String name = faker.get(key).dog().name();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog breed", input = InputType.YES, condition = InputType.NO)
    public void dogBreed() {
        try {
            String strObj = Input;
            String breed = faker.get(key).dog().breed();
            Report.updateTestLog(Action, "Generated data: " + breed, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, breed);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog sound", input = InputType.YES, condition = InputType.NO)
    public void dogSound() {
        try {
            String strObj = Input;
            String sound = faker.get(key).dog().sound();
            Report.updateTestLog(Action, "Generated data: " + sound, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sound);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog age", input = InputType.YES, condition = InputType.NO)
    public void dogAge() {
        try {
            String strObj = Input;
            String age = faker.get(key).dog().age();
            Report.updateTestLog(Action, "Generated data: " + age, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, age);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog coat length", input = InputType.YES, condition = InputType.NO)
    public void dogCoatLength() {
        try {
            String strObj = Input;
            String coatLength = faker.get(key).dog().coatLength();
            Report.updateTestLog(Action, "Generated data: " + coatLength, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, coatLength);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog size", input = InputType.YES, condition = InputType.NO)
    public void dogSize() {
        try {
            String strObj = Input;
            String size = faker.get(key).dog().size();
            Report.updateTestLog(Action, "Generated data: " + size, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, size);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog gender", input = InputType.YES, condition = InputType.NO)
    public void dogGender() {
        try {
            String strObj = Input;
            String gender = faker.get(key).dog().gender();
            Report.updateTestLog(Action, "Generated data: " + gender, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, gender);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dog meme phrase", input = InputType.YES, condition = InputType.NO)
    public void memePhrase() {
        try {
            String strObj = Input;
            String memePhrase = faker.get(key).dog().memePhrase();
            Report.updateTestLog(Action, "Generated data: " + memePhrase, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, memePhrase);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Dragon Ball character name", input = InputType.YES, condition = InputType.NO)
    public void characterDragonBall() {
        try {
            String strObj = Input;
            String character = faker.get(key).dragonBall().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random university name", input = InputType.YES, condition = InputType.NO)
    public void university() {
        try {
            String strObj = Input;
            String university = faker.get(key).educator().university();
            Report.updateTestLog(Action, "Generated data: " + university, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, university);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random course name", input = InputType.YES, condition = InputType.NO)
    public void course() {
        try {
            String strObj = Input;
            String course = faker.get(key).educator().course();
            Report.updateTestLog(Action, "Generated data: " + course, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, course);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random campus name", input = InputType.YES, condition = InputType.NO)
    public void campus() {
        try {
            String strObj = Input;
            String campus = faker.get(key).educator().campus();
            Report.updateTestLog(Action, "Generated data: " + campus, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, campus);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random secondary school name", input = InputType.YES, condition = InputType.NO)
    public void secondarySchool() {
        try {
            String strObj = Input;
            String secondarySchool = faker.get(key).educator().secondarySchool();
            Report.updateTestLog(Action, "Generated data: " + secondarySchool, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, secondarySchool);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random esports team name", input = InputType.YES, condition = InputType.NO)
    public void esportsTeam() {
        try {
            String strObj = Input;
            String team = faker.get(key).esports().team();
            Report.updateTestLog(Action, "Generated data: " + team, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, team);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random esports player name", input = InputType.YES, condition = InputType.NO)
    public void esportsPlayer() {
        try {
            String strObj = Input;
            String player = faker.get(key).esports().player();
            Report.updateTestLog(Action, "Generated data: " + player, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, player);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random esports game name", input = InputType.YES, condition = InputType.NO)
    public void esportsGame() {
        try {
            String strObj = Input;
            String game = faker.get(key).esports().game();
            Report.updateTestLog(Action, "Generated data: " + game, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, game);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random esports event name", input = InputType.YES, condition = InputType.NO)
    public void esportsEvent() {
        try {
            String strObj = Input;
            String event = faker.get(key).esports().event();
            Report.updateTestLog(Action, "Generated data: " + event, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, event);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random esports league", input = InputType.YES, condition = InputType.NO)
    public void esportsLeague() {
        try {
            String strObj = Input;
            String league = faker.get(key).esports().league();
            Report.updateTestLog(Action, "Generated data: " + league, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, league);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random file name", input = InputType.YES, condition = InputType.NO)
    public void fileName() {
        try {
            String strObj = Input;
            String fileName = faker.get(key).file().fileName();
            Report.updateTestLog(Action, "Generated data: " + fileName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, fileName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random file name with details", input = InputType.YES, condition = InputType.YES)
    public void fileNameWithDetails() {
        try {
            String strObj = Input;
            String dirOrNull = Condition.split(":", 4)[0];
            String nameOrNull = Condition.split(":", 4)[1];
            String extensionOrNull = Condition.split(":", 4)[2];
            String separatorOrNull = Condition.split(":", 4)[3];
            String fileName = faker.get(key).file().fileName(dirOrNull, nameOrNull, extensionOrNull, separatorOrNull);
            Report.updateTestLog(Action, "Generated data: " + fileName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, fileName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random mime type", input = InputType.YES, condition = InputType.NO)
    public void mimeType() {
        try {
            String strObj = Input;
            String mimeType = faker.get(key).file().mimeType();
            Report.updateTestLog(Action, "Generated data: " + mimeType, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, mimeType);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random file name with an optional file extension", input = InputType.NO)
    public void fileNameWithExtension() {
        try {
            String strObj = Input;
            String fileNameWithExtension = faker.get(key).file().extension();
            Report.updateTestLog(Action, "Generated data: " + fileNameWithExtension, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, fileNameWithExtension);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random credit card number", input = InputType.YES, condition = InputType.NO)
    public void financeCreditCardNumber() {
        try {
            String strObj = Input;
            String creditCard = faker.get(key).finance().creditCard();
            Report.updateTestLog(Action, "Generated data: " + creditCard, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, creditCard);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random credit card number based on type", input = InputType.YES, condition = InputType.YES)
    public void creditCardNumberBasedOnType() {
        try {
            String strObj = Input;
            String type = Condition;
            CreditCardType creditCardType = CreditCardType.valueOf(type);
            String creditCard = faker.get(key).finance().creditCard(creditCardType);
            Report.updateTestLog(Action, "Generated data: " + creditCard, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, creditCard);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random BIC (Bank Identifier Code)", input = InputType.YES, condition = InputType.NO)
    public void bic() {
        try {
            String strObj = Input;
            String bic = faker.get(key).finance().bic();
            Report.updateTestLog(Action, "Generated data: " + bic, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, bic);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IBAN (International Bank Account Number)", input = InputType.YES, condition = InputType.NO)
    public void iban() {
        try {
            String strObj = Input;
            String iban = faker.get(key).finance().iban();
            Report.updateTestLog(Action, "Generated data: " + iban, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, iban);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IBAN with a specific country code", input = InputType.YES, condition = InputType.YES)
    public void ibanByCountry() {
        try {
            String strObj = Input;
            String countryCode = Condition;
            String iban = faker.get(key).finance().iban(countryCode);
            Report.updateTestLog(Action, "Generated data: " + iban, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, iban);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ingredient", input = InputType.YES, condition = InputType.NO)
    public void ingredient() {
        try {
            String strObj = Input;
            String ingredient = faker.get(key).food().ingredient();
            Report.updateTestLog(Action, "Generated data: " + ingredient, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ingredient);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random spice", input = InputType.YES, condition = InputType.NO)
    public void spice() {
        try {
            String strObj = Input;
            String spice = faker.get(key).food().spice();
            Report.updateTestLog(Action, "Generated data: " + spice, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, spice);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random measurement", input = InputType.YES, condition = InputType.NO)
    public void measurement() {
        try {
            String strObj = Input;
            String measurement = faker.get(key).food().measurement();
            Report.updateTestLog(Action, "Generated data: " + measurement, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, measurement);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from the TV show \"Friends\"", input = InputType.YES, condition = InputType.NO)
    public void characterFriends() {
        try {
            String strObj = Input;
            String character = faker.get(key).friends().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random location from the TV show \"Friends\"", input = InputType.YES, condition = InputType.NO)
    public void locationFriends() {
        try {
            String strObj = Input;
            String location = faker.get(key).friends().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from the TV show \"Friends\"", input = InputType.YES, condition = InputType.NO)
    public void quoteFriends() {
        try {
            String strObj = Input;
            String quote = faker.get(key).friends().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random funny name", input = InputType.YES, condition = InputType.NO)
    public void funnyName() {
        try {
            String strObj = Input;
            String name = faker.get(key).funnyName().name();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from the Game of Thrones series", input = InputType.YES, condition = InputType.NO)
    public void characterGOT() {
        try {
            String strObj = Input;
            String character = faker.get(key).gameOfThrones().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random house name from the Game of Thrones series", input = InputType.YES, condition = InputType.NO)
    public void houseGOT() {
        try {
            String strObj = Input;
            String house = faker.get(key).gameOfThrones().house();
            Report.updateTestLog(Action, "Generated data: " + house, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, house);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random city name from the Game of Thrones series", input = InputType.YES, condition = InputType.NO)
    public void cityGOT() {
        try {
            String strObj = Input;
            String city = faker.get(key).gameOfThrones().city();
            Report.updateTestLog(Action, "Generated data: " + city, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, city);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random dragon name from the Game of Thrones series", input = InputType.YES, condition = InputType.NO)
    public void dragonGOT() {
        try {
            String strObj = Input;
            String dragon = faker.get(key).gameOfThrones().dragon();
            Report.updateTestLog(Action, "Generated data: " + dragon, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, dragon);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from the Game of Thrones series", input = InputType.YES, condition = InputType.NO)
    public void quoteGOT() {
        try {
            String strObj = Input;
            String quote = faker.get(key).gameOfThrones().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random abbreviation used in hacker slang", input = InputType.YES, condition = InputType.NO)
    public void hackerAbbreviation() {
        try {
            String strObj = Input;
            String abbreviation = faker.get(key).hacker().abbreviation();
            Report.updateTestLog(Action, "Generated data: " + abbreviation, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, abbreviation);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random adjective used in hacker slang", input = InputType.YES, condition = InputType.NO)
    public void hackerAdjective() {
        try {
            String strObj = Input;
            String adjective = faker.get(key).hacker().adjective();
            Report.updateTestLog(Action, "Generated data: " + adjective, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, adjective);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random noun used in hacker slang", input = InputType.YES, condition = InputType.NO)
    public void hackerNoun() {
        try {
            String strObj = Input;
            String noun = faker.get(key).hacker().noun();
            Report.updateTestLog(Action, "Generated data: " + noun, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, noun);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random verb used in hacker slang", input = InputType.YES, condition = InputType.NO)
    public void hackerVerb() {
        try {
            String strObj = Input;
            String verb = faker.get(key).hacker().verb();
            Report.updateTestLog(Action, "Generated data: " + verb, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, verb);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random ingverb used in hacker slang", input = InputType.YES, condition = InputType.NO)
    public void hackerIngVerb() {
        try {
            String strObj = Input;
            String ingVerb = faker.get(key).hacker().ingverb();
            Report.updateTestLog(Action, "Generated data: " + ingVerb, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ingVerb);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from the Harry Potter series", input = InputType.YES, condition = InputType.NO)
    public void characterHarryPotter() {
        try {
            String strObj = Input;
            String character = faker.get(key).harryPotter().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random location from the Harry Potter series", input = InputType.YES, condition = InputType.NO)
    public void locationHarryPotter() {
        try {
            String strObj = Input;
            String location = faker.get(key).harryPotter().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from the Harry Potter series", input = InputType.YES, condition = InputType.NO)
    public void quoteHarryPotter() {
        try {
            String strObj = Input;
            String quote = faker.get(key).harryPotter().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random book name from the Harry Potter series", input = InputType.YES, condition = InputType.NO)
    public void bookHarryPotter() {
        try {
            String strObj = Input;
            String book = faker.get(key).harryPotter().book();
            Report.updateTestLog(Action, "Generated data: " + book, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, book);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random word related to hipster culture", input = InputType.YES, condition = InputType.NO)
    public void hipsterWord() {
        try {
            String strObj = Input;
            String word = faker.get(key).hipster().word();
            Report.updateTestLog(Action, "Generated data: " + word, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, word);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void characterHitchhiker() {
        try {
            String strObj = Input;
            String character = faker.get(key).hitchhikersGuideToTheGalaxy().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random location name from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void locationHitchhiker() {
        try {
            String strObj = Input;
            String location = faker.get(key).hitchhikersGuideToTheGalaxy().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random marvin quote from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void marvinQuoteHitchhiker() {
        try {
            String strObj = Input;
            String quote = faker.get(key).hitchhikersGuideToTheGalaxy().marvinQuote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void quoteHitchhiker() {
        try {
            String strObj = Input;
            String quote = faker.get(key).hitchhikersGuideToTheGalaxy().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random planet name from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void planetHitchhiker() {
        try {
            String strObj = Input;
            String planet = faker.get(key).hitchhikersGuideToTheGalaxy().planet();
            Report.updateTestLog(Action, "Generated data: " + planet, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, planet);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random species name from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void specieHitchhiker() {
        try {
            String strObj = Input;
            String species = faker.get(key).hitchhikersGuideToTheGalaxy().specie();
            Report.updateTestLog(Action, "Generated data: " + species, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, species);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random starship from the Hitchhiker's Guide to the Galaxy series", input = InputType.YES, condition = InputType.NO)
    public void starshipHitchhiker() {
        try {
            String strObj = Input;
            String starShip = faker.get(key).hitchhikersGuideToTheGalaxy().starship();
            Report.updateTestLog(Action, "Generated data: " + starShip, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, starShip);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from The Hobbit series", input = InputType.YES, condition = InputType.NO)
    public void characterHobbit() {
        try {
            String strObj = Input;
            String character = faker.get(key).hobbit().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random location name from The Hobbit series", input = InputType.YES, condition = InputType.NO)
    public void locationHobbit() {
        try {
            String strObj = Input;
            String location = faker.get(key).hobbit().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from The Hobbit series", input = InputType.YES, condition = InputType.NO)
    public void quoteHobbit() {
        try {
            String strObj = Input;
            String quote = faker.get(key).hobbit().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random hobbit Thorins Company from The Hobbit series", input = InputType.YES, condition = InputType.NO)
    public void thorinsCompanyHobbit() {
        try {
            String strObj = Input;
            String company = faker.get(key).hobbit().thorinsCompany();
            Report.updateTestLog(Action, "Generated data: " + company, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, company);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from How I Met Your Mother", input = InputType.YES, condition = InputType.NO)
    public void characterHowIMetYourMother() {
        try {
            String strObj = Input;
            String character = faker.get(key).howIMetYourMother().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from How I Met Your Mother", input = InputType.YES, condition = InputType.NO)
    public void quoteHowIMetYourMother() {
        try {
            String strObj = Input;
            String quote = faker.get(key).howIMetYourMother().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random catchphrase from How I Met Your Mother", input = InputType.YES, condition = InputType.NO)
    public void catchPhraseHowIMetYourMother() {
        try {
            String strObj = Input;
            String catchphrase = faker.get(key).howIMetYourMother().catchPhrase();
            Report.updateTestLog(Action, "Generated data: " + catchphrase, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, catchphrase);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random high five from How I Met Your Mother", input = InputType.YES, condition = InputType.NO)
    public void highFiveHowIMetYourMother() {
        try {
            String strObj = Input;
            String highFive = faker.get(key).howIMetYourMother().highFive();
            Report.updateTestLog(Action, "Generated data: " + highFive, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, highFive);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a valid random ID number", input = InputType.YES, condition = InputType.NO)
    public void validIdNumber() {
        try {
            String strObj = Input;
            String validId = faker.get(key).idNumber().valid();
            Report.updateTestLog(Action, "Generated data: " + validId, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, validId);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating valid ID: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate an invalid random ID number", input = InputType.YES, condition = InputType.NO)
    public void invalidIdNumber() {
        try {
            String strObj = Input;
            String invalidId = faker.get(key).idNumber().invalid();
            Report.updateTestLog(Action, "Generated data: " + invalidId, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, invalidId);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating invalid ID: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random valid SSN", input = InputType.YES, condition = InputType.NO)
    public void validSsn() {
        try {
            String strObj = Input;
            String validSsn = faker.get(key).idNumber().validSvSeSsn();
            Report.updateTestLog(Action, "Generated data: " + validSsn, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, validSsn);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating valid SSN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random invalid SSN", input = InputType.YES, condition = InputType.NO)
    public void invalidSsn() {
        try {
            String strObj = Input;
            String invalidSsn = faker.get(key).idNumber().invalidSvSeSsn();
            Report.updateTestLog(Action, "Generated data: " + invalidSsn, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, invalidSsn);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating invalid SSN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random valid SSN", input = InputType.YES, condition = InputType.NO)
    public void ssnValid() {
        try {
            String strObj = Input;
            String ssnValid = faker.get(key).idNumber().ssnValid();
            Report.updateTestLog(Action, "Generated data: " + ssnValid, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ssnValid);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating valid SSN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random email address", input = InputType.YES, condition = InputType.NO)
    public void emailAddress() {
        try {
            String strObj = Input;
            String email = faker.get(key).internet().emailAddress();
            Report.updateTestLog(Action, "Generated data: " + email, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, email);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating email: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random email address with local part", input = InputType.YES, condition = InputType.YES)
    public void emailAddressWithLocalPart() {
        try {
            String strObj = Input;
            String localPart = Condition;
            String email = faker.get(key).internet().emailAddress(localPart);
            Report.updateTestLog(Action, "Generated data: " + email, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, email);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating email: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random safe email address", input = InputType.YES, condition = InputType.NO)
    public void safeEmailAddress() {
        try {
            String strObj = Input;
            String safeEmail = faker.get(key).internet().safeEmailAddress();
            Report.updateTestLog(Action, "Generated data: " + safeEmail, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, safeEmail);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating safe email: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random safe email address with local part", input = InputType.YES, condition = InputType.YES)
    public void safeEmailAddressWithLocalPart() {
        try {
            String strObj = Input;
            String localPart = Condition;
            String safeEmail = faker.get(key).internet().safeEmailAddress(localPart);
            Report.updateTestLog(Action, "Generated data: " + safeEmail, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, safeEmail);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating safe email: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random domain name", input = InputType.YES, condition = InputType.NO)
    public void domainName() {
        try {
            String strObj = Input;
            String domainName = faker.get(key).internet().domainName();
            Report.updateTestLog(Action, "Generated data: " + domainName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, domainName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating domain name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random domain suffix", input = InputType.YES, condition = InputType.NO)
    public void domainSuffix() {
        try {
            String strObj = Input;
            String domainSuffix = faker.get(key).internet().domainSuffix();
            Report.updateTestLog(Action, "Generated data " + domainSuffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, domainSuffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating domain name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random domain Word", input = InputType.YES, condition = InputType.NO)
    public void domainWord() {
        try {
            String strObj = Input;
            String domainWord = faker.get(key).internet().domainWord();
            Report.updateTestLog(Action, "Generated data: " + domainWord, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, domainWord);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating domain name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random URL", input = InputType.YES, condition = InputType.NO)
    public void internetUrl() {
        try {
            String strObj = Input;
            String url = faker.get(key).internet().url();
            Report.updateTestLog(Action, "Generated data: " + url, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, url);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating URL: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IP (IPv4) address", input = InputType.YES, condition = InputType.NO)
    public void ipV4Address() {
        try {
            String strObj = Input;
            String ipV4 = faker.get(key).internet().ipV4Address();
            Report.updateTestLog(Action, "Generated data: " + ipV4, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ipV4);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv4 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IP (IPv6) address", input = InputType.YES, condition = InputType.NO)
    public void ipV6Address() {
        try {
            String strObj = Input;
            String ipV6 = faker.get(key).internet().ipV6Address();
            Report.updateTestLog(Action, "Generated data: " + ipV6, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ipV6);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv6 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IP (IPv4) CIDR address", input = InputType.YES, condition = InputType.NO)
    public void ipV4Cidr() {
        try {
            String strObj = Input;
            String ipV4 = faker.get(key).internet().ipV4Cidr();
            Report.updateTestLog(Action, "Generated data: " + ipV4, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ipV4);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv4 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random IP (IPv6) CIDR address", input = InputType.YES, condition = InputType.NO)
    public void ipV6Cidr() {
        try {
            String strObj = Input;
            String ipV6 = faker.get(key).internet().ipV6Cidr();
            Report.updateTestLog(Action, "Generated data: " + ipV6, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ipV6);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv6 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random MAC address", input = InputType.YES, condition = InputType.NO)
    public void macAddress() {
        try {
            String strObj = Input;
            String macAddress = faker.get(key).internet().macAddress();
            Report.updateTestLog(Action, "Generated data: " + macAddress, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, macAddress);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating MAC Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random MAC address with Prefix", input = InputType.YES, condition = InputType.NO)
    public void macAddressWithPrefix() {
        try {
            String strObj = Input;
            String prefix = "A32";
            String macAddress = faker.get(key).internet().macAddress(prefix);
            Report.updateTestLog(Action, "Generated data: " + macAddress, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, macAddress);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating MAC Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random private IP (IPv4) address", input = InputType.YES, condition = InputType.NO)
    public void privateIpV4Address() {
        try {
            String strObj = Input;
            String ipV4 = faker.get(key).internet().privateIpV4Address();
            Report.updateTestLog(Action, "Generated data: " + ipV4, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ipV4);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv4 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random public IP (IPv6) address", input = InputType.YES, condition = InputType.NO)
    public void publicIpV6Address() {
        try {
            String strObj = Input;
            String ipV6 = faker.get(key).internet().publicIpV4Address();
            Report.updateTestLog(Action, "Generated data: " + ipV6, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, ipV6);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv6 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random internet avatar", input = InputType.YES, condition = InputType.NO)
    public void internetAvatar() {
        try {
            String strObj = Input;
            String avatar = faker.get(key).internet().avatar();
            Report.updateTestLog(Action, "Generated data: " + avatar, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, avatar);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv6 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random internet image", input = InputType.YES, condition = InputType.NO)
    public void internetImage() {
        try {
            String strObj = Input;
            String image = faker.get(key).internet().image();
            Report.updateTestLog(Action, "Generated data: " + image, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, image);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv6 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random internet image with specs", input = InputType.YES, condition = InputType.YES)
    public void internetImageWithSpecs() {
        try {
            String strObj = Input;
            String widthStr = Condition.split(":", 4)[0];
            String heightStr = Condition.split(":", 4)[1];
            String grayStr = Condition.split(":", 4)[2];
            String text = Condition.split(":", 4)[3];
            Integer width = Integer.parseInt(widthStr);
            Integer height = Integer.parseInt(heightStr);
            Boolean gray = Boolean.valueOf(grayStr);
            String image = faker.get(key).internet().image(width, height, gray, text);
            Report.updateTestLog(Action, "Generated data: " + image, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, image);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating IPv6 Address: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random password with custom length", input = InputType.YES, condition = InputType.YES)
    public void internetPasswordWithLength() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minLength = Integer.parseInt(minStr);
            int maxLength = Integer.parseInt(maxStr);
            String password = faker.get(key).internet().password(minLength, maxLength);
            Report.updateTestLog(Action, "Generated data: " + password, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, password);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random password", input = InputType.YES, condition = InputType.NO)
    public void internetPassword() {
        try {
            String strObj = Input;
            String password = faker.get(key).internet().password();
            Report.updateTestLog(Action, "Generated data: " + password, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, password);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random password including digits", input = InputType.YES, condition = InputType.NO)
    public void internetPasswordWithDigits() {
        try {
            String strObj = Input;
            boolean includeDigit = true;
            String password = faker.get(key).internet().password(includeDigit);
            Report.updateTestLog(Action, "Generated data: " + password, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, password);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random password with custom length and uppercase included", input = InputType.YES, condition = InputType.YES)
    public void internetPasswordWithLengthUppercase() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minLength = Integer.parseInt(minStr);
            int maxLength = Integer.parseInt(maxStr);
            boolean includeUppercase = true;
            String password = faker.get(key).internet().password(minLength, maxLength, includeUppercase);
            Report.updateTestLog(Action, "Generated data: " + password, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, password);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random password with custom length, uppercase and special character", input = InputType.YES, condition = InputType.YES)
    public void internetPasswordWithLengthUppercaseSpecial() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minLength = Integer.parseInt(minStr);
            int maxLength = Integer.parseInt(maxStr);
            boolean includeUppercase = true;
            boolean includeSpecial = true;
            String password = faker.get(key).internet().password(minLength, maxLength, includeUppercase, includeSpecial);
            Report.updateTestLog(Action, "Generated data: " + password, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, password);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random password with custom length, uppercase, special character and digits", input = InputType.YES, condition = InputType.YES)
    public void internetPasswordwithUppercaseSpecialDigit() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minLength = Integer.parseInt(minStr);
            int maxLength = Integer.parseInt(maxStr);
            boolean includeUppercase = true;
            boolean includeSpecial = true;
            boolean includeDigit = true;
            String password = faker.get(key).internet().password(minLength, maxLength, includeUppercase, includeSpecial, includeDigit);
            Report.updateTestLog(Action, "Generated data: " + password, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, password);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating password: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random slug (like a URL slug)", input = InputType.YES, condition = InputType.NO)
    public void slug() {
        try {
            String strObj = Input;
            String slug = faker.get(key).internet().slug();
            Report.updateTestLog(Action, "Generated data: " + slug, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, slug);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating slug: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random slug (like a URL slug) with specs", input = InputType.YES, condition = InputType.YES)
    public void slugWithSpecs() {
        try {
            String strObj = Input;
            List<String> wordsOrNull = new ArrayList<>();
            char splitChar = ':';
            int count = (int) Condition.chars().filter(ch -> ch == splitChar).count();
            for (int i = 0; i < count; i++) {
                wordsOrNull.add(Condition.split(":", count + 1)[i]);
            }
            String glueOrNull = Condition.split(":", count + 1)[count];
            String slug = faker.get(key).internet().slug(wordsOrNull, glueOrNull);
            Report.updateTestLog(Action, "Generated data: " + slug, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, slug);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating slug: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random user agent with Agent type", input = InputType.YES, condition = InputType.YES)
    public void userAgentWithAgentType() {
        try {
            String strObj = Input;
            String option = Condition;
            Internet.UserAgent userAgent = Internet.UserAgent.valueOf(option);
            String userAgent1 = faker.get(key).internet().userAgent(userAgent);
            Report.updateTestLog(Action, "Generated data: " + userAgent1, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, userAgent1);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating slug: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random user agent", input = InputType.YES, condition = InputType.NO)
    public void userAgentAny() {
        try {
            String strObj = Input;
            String userAgentAny = faker.get(key).internet().userAgentAny();
            Report.updateTestLog(Action, "Generated data: " + userAgentAny, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, userAgentAny);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating slug: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate any random uuid", input = InputType.YES, condition = InputType.NO)
    public void internetUUID() {
        try {
            String strObj = Input;
            String uuid = faker.get(key).internet().uuid();
            Report.updateTestLog(Action, "Generated data: " + uuid, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, uuid);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating slug: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random job title", input = InputType.YES, condition = InputType.NO)
    public void jobTitle() {
        try {
            String strObj = Input;
            String jobTitle = faker.get(key).job().title();
            Report.updateTestLog(Action, "Generated data: " + jobTitle, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, jobTitle);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating job title: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random field of job", input = InputType.YES, condition = InputType.NO)
    public void jobField() {
        try {
            String strObj = Input;
            String jobField = faker.get(key).job().field();
            Report.updateTestLog(Action, "Generated data: " + jobField, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, jobField);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating job field: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random key skill for the job", input = InputType.YES, condition = InputType.NO)
    public void jobKeySkills() {
        try {
            String strObj = Input;
            String keySkill = faker.get(key).job().keySkills();
            Report.updateTestLog(Action, "Generated data: " + keySkill, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, keySkill);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating job key skill: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random seniority level for the job", input = InputType.YES, condition = InputType.NO)
    public void jobSeniority() {
        try {
            String strObj = Input;
            String seniority = faker.get(key).job().seniority();
            Report.updateTestLog(Action, "Generated data: " + seniority, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, seniority);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating job seniority: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random job position", input = InputType.YES, condition = InputType.NO)
    public void jobPosition() {
        try {
            String strObj = Input;
            String position = faker.get(key).job().position();
            Report.updateTestLog(Action, "Generated data: " + position, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, position);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating job position: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random League of Legends champion", input = InputType.YES, condition = InputType.NO)
    public void championLOL() {
        try {
            String strObj = Input;
            String champion = faker.get(key).leagueOfLegends().champion();
            Report.updateTestLog(Action, "Generated data: " + champion, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, champion);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating champion: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random League of Legends summoner spell", input = InputType.YES, condition = InputType.NO)
    public void summonerSpellLOL() {
        try {
            String strObj = Input;
            String summonerSpell = faker.get(key).leagueOfLegends().summonerSpell();
            Report.updateTestLog(Action, "Generated data: " + summonerSpell, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, summonerSpell);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating summoner spell: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random League of Legends masteries", input = InputType.YES, condition = InputType.NO)
    public void masteriesLOL() {
        try {
            String strObj = Input;
            String mastery = faker.get(key).leagueOfLegends().masteries();
            Report.updateTestLog(Action, "Generated data: " + mastery, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, mastery);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating mastery: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random League of Legends quote", input = InputType.YES, condition = InputType.NO)
    public void quoteLOL() {
        try {
            String strObj = Input;
            String quote = faker.get(key).leagueOfLegends().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random League of Legends rank", input = InputType.YES, condition = InputType.NO)
    public void rankLOL() {
        try {
            String strObj = Input;
            String rank = faker.get(key).leagueOfLegends().rank();
            Report.updateTestLog(Action, "Generated data: " + rank, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, rank);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating rank: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random League of Legends location", input = InputType.YES, condition = InputType.NO)
    public void locationLOL() {
        try {
            String strObj = Input;
            String location = faker.get(key).leagueOfLegends().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating rank: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random character name from The Big Lebowski", input = InputType.YES, condition = InputType.NO)
    public void characterLebowski() {
        try {
            String strObj = Input;
            String character = faker.get(key).lebowski().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lebowski character: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random quote from The Big Lebowski", input = InputType.YES, condition = InputType.NO)
    public void quoteLebowski() {
        try {
            String strObj = Input;
            String quote = faker.get(key).lebowski().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lebowski quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random actor from The Big Lebowski", input = InputType.YES, condition = InputType.NO)
    public void actorLebowski() {
        try {
            String strObj = Input;
            String actor = faker.get(key).lebowski().actor();
            Report.updateTestLog(Action, "Generated data: " + actor, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, actor);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lebowski quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lord of the Rings character name", input = InputType.YES, condition = InputType.NO)
    public void characterLordOfTheRings() {
        try {
            String strObj = Input;
            String character = faker.get(key).lordOfTheRings().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lord of the Rings character: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lord of the Rings location", input = InputType.YES, condition = InputType.NO)
    public void locationLordOfTheRings() {
        try {
            String strObj = Input;
            String location = faker.get(key).lordOfTheRings().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lord of the Rings location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem word", input = InputType.YES, condition = InputType.NO)
    public void loremWord() {
        try {
            String strObj = Input;
            String word = faker.get(key).lorem().word();
            Report.updateTestLog(Action, "Generated data: " + word, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, word);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem word: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem words", input = InputType.YES, condition = InputType.NO)
    public void loremWords() {
        try {
            String strObj = Input;
            List<String> word = faker.get(key).lorem().words();
            Report.updateTestLog(Action, "Generated data: " + word, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, word.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem word: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a list of random Lorem words", input = InputType.YES, condition = InputType.YES)
    public void loremWordsWithCount() {
        try {
            String strObj = Input;
            String countStr = Condition;
            int count = Integer.parseInt(countStr);
            List<String> words = faker.get(key).lorem().words(count);
            Report.updateTestLog(Action, "Generated data: " + words, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, words.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem words: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem sentence", input = InputType.YES, condition = InputType.NO)
    public void sentence() {
        try {
            String strObj = Input;
            String sentence = faker.get(key).lorem().sentence();
            Report.updateTestLog(Action, "Generated data: " + sentence, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sentence);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem sentence: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem sentence with a given word count", input = InputType.YES, condition = InputType.YES)
    public void sentenceWithCount() {
        try {
            String strObj = Input;
            String wordCountStr = Condition;
            int wordCount = Integer.parseInt(wordCountStr);
            String sentence = faker.get(key).lorem().sentence(wordCount);
            Report.updateTestLog(Action, "Generated data: " + sentence + ": " + sentence, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sentence);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem sentence: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem sentence with a given word count and words to add", input = InputType.YES, condition = InputType.YES)
    public void sentenceWithCountAndRandomWords() {
        try {
            String strObj = Input;
            String wordCountStr = Condition.split(":", 2)[0];
            String wordsToAddStr = Condition.split(":", 2)[1];
            int wordCount = Integer.parseInt(wordCountStr);
            int randomWordsToAdd = Integer.parseInt(wordsToAddStr);
            String sentence = faker.get(key).lorem().sentence(wordCount, randomWordsToAdd);
            Report.updateTestLog(Action, "Generated data: " + sentence + ": " + sentence, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sentence);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem sentence: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem sentence with a given sentence count", input = InputType.YES, condition = InputType.YES)
    public void sentences() {
        try {
            String strObj = Input;
            String sentenceCountStr = Condition;
            int sentenceCount = Integer.parseInt(sentenceCountStr);
            String sentence = faker.get(key).lorem().sentence(sentenceCount);
            Report.updateTestLog(Action, "Generated data: " + sentence + ": " + sentence, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sentence);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem sentence: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem paragraph", input = InputType.YES, condition = InputType.NO)
    public void paragraph() {
        try {
            String strObj = Input;
            String paragraph = faker.get(key).lorem().paragraph();
            Report.updateTestLog(Action, "Generated data: " + paragraph, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, paragraph);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraph: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem paragraph with a given sentence count", input = InputType.YES, condition = InputType.YES)
    public void paragraphWithSentenceCount() {
        try {
            String strObj = Input;
            String sentenceCountStr = Condition;
            int sentenceCount = Integer.parseInt(sentenceCountStr);
            String paragraph = faker.get(key).lorem().paragraph(sentenceCount);
            Report.updateTestLog(Action, "Generated data: " + sentenceCount + ": " + paragraph, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, paragraph);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraph: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a list of random Lorem paragraphs", input = InputType.YES, condition = InputType.YES)
    public void paragraphsWithParagraphCount() {
        try {
            String strObj = Input;
            String paragraphCountStr = Condition;
            int paragraphCount = Integer.parseInt(paragraphCountStr);
            List<String> paragraphs = faker.get(key).lorem().paragraphs(paragraphCount);
            Report.updateTestLog(Action, "Generated data: " + paragraphs, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, paragraphs.toString());
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem character", input = InputType.YES, condition = InputType.NO)
    public void loremCharacter() {
        try {
            String strObj = Input;
            char character = faker.get(key).lorem().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Character.toString(character));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters", input = InputType.YES, condition = InputType.NO)
    public void loremCharacters() {
        try {
            String strObj = Input;
            String characters = faker.get(key).lorem().characters();
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem character including uppercase", input = InputType.YES, condition = InputType.NO)
    public void loremCharacterIncludeUpperCase() {
        try {
            String strObj = Input;
            boolean includeUppercase = true;
            char character = faker.get(key).lorem().character(includeUppercase);
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Character.toString(character));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters including uppercase", input = InputType.YES, condition = InputType.NO)
    public void loremCharactersIncludeUpperCase() {
        try {
            String strObj = Input;
            boolean includeUppercase = true;
            String characters = faker.get(key).lorem().characters(includeUppercase);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters with fixed number of characters", input = InputType.YES, condition = InputType.YES)
    public void loremCharactersWithNumberOfChars() {
        try {
            String strObj = Input;
            String charsStr = Condition;
            int fixedNumberOfCharacters = Integer.parseInt(charsStr);
            String characters = faker.get(key).lorem().characters(fixedNumberOfCharacters);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters with fixed number of characters and uppercase", input = InputType.YES, condition = InputType.YES)
    public void loremCharactersWithNumberOfCharsUpperCase() {
        try {
            String strObj = Input;
            String charsStr = Condition;
            int fixedNumberOfCharacters = Integer.parseInt(charsStr);
            boolean includeUppercase = true;
            String characters = faker.get(key).lorem().characters(fixedNumberOfCharacters, includeUppercase);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters with fixed number of characters, uppercase and digit", input = InputType.YES, condition = InputType.YES)
    public void loremCharactersWithNumberOfCharsUpperCaseDigit() {
        try {
            String strObj = Input;
            String charsStr = Condition;
            int fixedNumberOfCharacters = Integer.parseInt(charsStr);
            boolean includeUppercase = true;
            boolean includeDigit = true;
            String characters = faker.get(key).lorem().characters(fixedNumberOfCharacters, includeUppercase, includeDigit);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters with min and max length", input = InputType.YES, condition = InputType.YES)
    public void loremCharactersWithinLength() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minimumLength = Integer.parseInt(minStr);
            int maximumLength = Integer.parseInt(maxStr);
            String characters = faker.get(key).lorem().characters(minimumLength, maximumLength);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random Lorem characters with min and max length including uppercase", input = InputType.YES, condition = InputType.YES)
    public void loremCharactersWithinLengthUpperCase() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minimumLength = Integer.parseInt(minStr);
            int maximumLength = Integer.parseInt(maxStr);
            boolean includeUppercase = true;
            String characters = faker.get(key).lorem().characters(minimumLength, maximumLength, includeUppercase);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a list of random Lorem characters with min and max length including uppercase and digits", input = InputType.YES, condition = InputType.YES)
    public void loremCharactersWithinLengthUpperCaseDigit() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int minimumLength = Integer.parseInt(minStr);
            int maximumLength = Integer.parseInt(maxStr);
            boolean includeUppercase = true;
            boolean includeDigit = true;
            String characters = faker.get(key).lorem().characters(minimumLength, maximumLength, includeUppercase, includeDigit);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Lorem fixed string", input = InputType.YES, condition = InputType.YES)
    public void loremFixedString() {
        try {
            String strObj = Input;
            String countStr = Condition;
            int numberOfLetters = Integer.parseInt(countStr);
            String characters = faker.get(key).lorem().characters(numberOfLetters);
            Report.updateTestLog(Action, "Generated data: " + characters, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, characters);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Lorem paragraphs: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Matz quote", input = InputType.YES, condition = InputType.NO)
    public void quoteMatz() {
        try {
            String strObj = Input;
            String quote = faker.get(key).matz().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Matz quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random medical disease name", input = InputType.YES, condition = InputType.NO)
    public void diseaseName() {
        try {
            String strObj = Input;
            String disease = faker.get(key).medical().diseaseName();
            Report.updateTestLog(Action, "Generated data: " + disease, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, disease);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Medical disease name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random medical hospital name", input = InputType.YES, condition = InputType.NO)
    public void hospitalName() {
        try {
            String strObj = Input;
            String hospital = faker.get(key).medical().hospitalName();
            Report.updateTestLog(Action, "Generated data: " + hospital, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hospital);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Medical hospital name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random medical medication name", input = InputType.YES, condition = InputType.NO)
    public void medicineName() {
        try {
            String strObj = Input;
            String medication = faker.get(key).medical().medicineName();
            Report.updateTestLog(Action, "Generated data: " + medication, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, medication);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Medical medication name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random medical symptom", input = InputType.YES, condition = InputType.NO)
    public void symptoms() {
        try {
            String strObj = Input;
            String symptom = faker.get(key).medical().symptoms();
            Report.updateTestLog(Action, "Generated data: " + symptom, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, symptom);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Medical symptom: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random music genre", input = InputType.YES, condition = InputType.NO)
    public void musicGenre() {
        try {
            String strObj = Input;
            String genre = faker.get(key).music().genre();
            Report.updateTestLog(Action, "Generated data: " + genre, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, genre);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Music genre: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random musical instrument", input = InputType.YES, condition = InputType.NO)
    public void musicalInstrument() {
        try {
            String strObj = Input;
            String instrument = faker.get(key).music().instrument();
            Report.updateTestLog(Action, "Generated data: " + instrument, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, instrument);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Music instrument: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random music chord", input = InputType.YES, condition = InputType.NO)
    public void musicChord() {
        try {
            String strObj = Input;
            String chord = faker.get(key).music().chord();
            Report.updateTestLog(Action, "Generated data: " + chord, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, chord);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Music band: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random music key", input = InputType.YES, condition = InputType.NO)
    public void musicKey() {
        try {
            String strObj = Input;
            String musicKey = faker.get(key).music().key();
            Report.updateTestLog(Action, "Generated data: " + key, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, key);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Music artist: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random first name", input = InputType.YES, condition = InputType.NO)
    public void firstName() {
        try {
            String strObj = Input;
            String firstName = faker.get(key).name().firstName();
            Report.updateTestLog(Action, "Generated data: " + firstName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, firstName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating first name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random last name", input = InputType.YES, condition = InputType.NO)
    public void lastName() {
        try {
            String strObj = Input;
            String lastName = faker.get(key).name().lastName();
            Report.updateTestLog(Action, "Generated data: " + lastName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, lastName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating last name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random full name", input = InputType.YES, condition = InputType.NO)
    public void fullName() {
        try {
            String strObj = Input;
            String fullName = faker.get(key).name().fullName();
            Report.updateTestLog(Action, "Generated data: " + fullName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, fullName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating full name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random name with prefix", input = InputType.YES, condition = InputType.NO)
    public void namePrefix() {
        try {
            String strObj = Input;
            String prefix = faker.get(key).name().prefix();
            Report.updateTestLog(Action, "Generated data: " + prefix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, prefix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating name with prefix: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random name with suffix", input = InputType.YES, condition = InputType.NO)
    public void nameSuffix() {
        try {
            String strObj = Input;
            String suffix = faker.get(key).name().suffix();
            Report.updateTestLog(Action, "Generated data: " + suffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, suffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating name with suffix: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random name title", input = InputType.YES, condition = InputType.NO)
    public void nameTitle() {
        try {
            String strObj = Input;
            String title = faker.get(key).name().title();
            Report.updateTestLog(Action, "Generated data: " + title, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, title);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating title: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random username", input = InputType.YES, condition = InputType.NO)
    public void username() {
        try {
            String strObj = Input;
            String username = faker.get(key).name().username();
            Report.updateTestLog(Action, "Generated data: " + username, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, username);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating username: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    //check this name()
    @Action(object = ObjectType.DATA, desc = "Generate a random name", input = InputType.YES, condition = InputType.NO)
    public void name() {
        try {
            String strObj = Input;
            String name = faker.get(key).name().name();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating username: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random name with middle name", input = InputType.YES, condition = InputType.NO)
    public void nameWithMiddleName() {
        try {
            String strObj = Input;
            String name = faker.get(key).name().nameWithMiddle();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating username: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random digit", input = InputType.YES, condition = InputType.NO)
    public void digit() {
        try {
            String strObj = Input;
            String digit = faker.get(key).number().digit();
            Report.updateTestLog(Action, "Generated data: " + digit, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, digit);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating digit: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate random digits", input = InputType.YES, condition = InputType.YES)
    public void digits() {
        try {
            String strObj = Input;
            String countStr = Condition;
            int count = Integer.parseInt(countStr);
            String digit = faker.get(key).number().digits(count);
            Report.updateTestLog(Action, "Generated data: " + digit, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, digit);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating digits: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random number", input = InputType.YES, condition = InputType.NO)
    public void randomNumber() {
        try {
            String strObj = Input;
            long randomNumber = faker.get(key).number().randomNumber();
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random number from 0-9 (both inclusive)", input = InputType.YES, condition = InputType.NO)
    public void randomDigit() {
        try {
            String strObj = Input;
            int randomDigit = faker.get(key).number().randomDigit();
            Report.updateTestLog(Action, "Generated data: " + randomDigit, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Integer.toString(randomDigit));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random digit: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random number from 1-9 (both inclusive)", input = InputType.YES, condition = InputType.NO)
    public void randomDigitNot0() {
        try {
            String strObj = Input;
            int randomDigit = faker.get(key).number().randomDigitNotZero();
            Report.updateTestLog(Action, "Generated data: " + randomDigit, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Integer.toString(randomDigit));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random digit: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random number between 2 integers", input = InputType.YES, condition = InputType.YES)
    public void randomIntegerNumberBetween() {
        try {
            String strObj = Input;
            String inputMin = Condition.split(":", 2)[0];
            String inputMax = Condition.split(":", 2)[1];
            int min = Integer.parseInt(inputMin);
            int max = Integer.parseInt(inputMax);
            int randomNumber = faker.get(key).number().numberBetween(min, max);
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Integer.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random number between 2 long numbers", input = InputType.YES, condition = InputType.YES)
    public void randomLongNumberBetween() {
        try {
            String strObj = Input;
            String inputMin = Condition.split(":", 2)[0];
            String inputMax = Condition.split(":", 2)[1];
            long min = Long.parseLong(inputMin);
            long max = Long.parseLong(inputMax);
            long randomNumber = faker.get(key).number().numberBetween(min, max);
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random double between 2 integers with maximum number of decimals", input = InputType.YES, condition = InputType.YES)
    public void randomDoubleBetweenIntegers() {
        try {
            String strObj = Input;
            String inputMaxDecimals = Condition.split(":", 3)[0];
            String inputMin = Condition.split(":", 3)[1];
            String inputMax = Condition.split(":", 3)[2];
            int maxNumOfDecimals = Integer.parseInt(inputMaxDecimals);
            int min = Integer.parseInt(inputMin);
            int max = Integer.parseInt(inputMax);
            Double randomNumber = faker.get(key).number().randomDouble(maxNumOfDecimals, min, max);
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Double.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random double between 2 Long numbers with maximum number of decimals", input = InputType.YES, condition = InputType.YES)
    public void randomDoubleBetweenLong() {
        try {
            String strObj = Input;
            String inputMaxDecimals = Condition.split(":", 3)[0];
            String inputMin = Condition.split(":", 3)[1];
            String inputMax = Condition.split(":", 3)[2];
            int maxNumOfDecimals = Integer.parseInt(inputMaxDecimals);
            long min = Long.parseLong(inputMin);
            long max = Long.parseLong(inputMax);
            Double randomNumber = faker.get(key).number().randomDouble(maxNumOfDecimals, min, max);
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Double.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random number between 1 and 100: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random number with specific number of digits", input = InputType.YES, condition = InputType.YES)
    public void randomNumberWithNoOfDigits() {
        try {
            String strObj = Input;
            String digitStr = Condition;
            int numOfDigits = Integer.parseInt(digitStr);
            boolean strict = true;
            long randomNumber = faker.get(key).number().randomNumber(numOfDigits, strict);
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Pick a random boolean value (true or false)", input = InputType.YES, condition = InputType.NO)
    public void optionFromBoolean() {
        try {
            String strObj = Input;
            boolean randomBoolean = faker.get(key).options().option(Boolean.TRUE, Boolean.FALSE);
            Report.updateTestLog(Action, "Generated data: " + randomBoolean, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Boolean.toString(randomBoolean));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating boolean: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Pick a random value from a predefined list of integers", input = InputType.YES, condition = InputType.YES)
    public void optionFromIntegers() {
        try {
            String strObj = Input;
            char splitChar = ':';
            int count = (int) Condition.chars().filter(ch -> ch == splitChar).count();
            Integer[] numbersList = new Integer[count + 1];
            for (int i = 0; i <= count; i++) {
                numbersList[i] = Integer.valueOf(Condition.split(":", count + 1)[i]);
            }
            Integer randomNumber = faker.get(key).options().option(numbersList);
            Report.updateTestLog(Action, "Generated data: " + randomNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomNumber));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating integer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Pick a random value from a predefined list of doubles", input = InputType.YES, condition = InputType.YES)
    public void optionFromDoubles() {
        try {
            String strObj = Input;
            char splitChar = ':';
            int count = (int) Condition.chars().filter(ch -> ch == splitChar).count();
            Double[] doubleList = new Double[count + 1];
            for (int i = 0; i <= count; i++) {
                doubleList[i] = Double.valueOf(Condition.split(":", count + 1)[i]);
            }
            Double randomDouble = faker.get(key).options().option(doubleList);
            Report.updateTestLog(Action, "Generated data: " + randomDouble, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Double.toString(randomDouble));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating double: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Pick a random value from a predefined list of Longs", input = InputType.YES, condition = InputType.YES)
    public void optionFromLong() {
        try {
            String strObj = Input;
            char splitChar = ':';
            int count = (int) Condition.chars().filter(ch -> ch == splitChar).count();
            Long[] longList = new Long[count + 1];
            for (int i = 0; i <= count; i++) {
                longList[i] = Long.valueOf(Condition.split(":", count + 1)[i]);
            }
            Long randomDouble = faker.get(key).options().option(longList);
            Report.updateTestLog(Action, "Generated data: " + randomDouble, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomDouble));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating double: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Pick a random value from a predefined list of strings", input = InputType.YES, condition = InputType.YES)
    public void optionFromStrings() {
        try {
            String strObj = Input;
            char splitChar = ':';
            int count = (int) Condition.chars().filter(ch -> ch == splitChar).count();
            String[] stringList = new String[count + 1];
            for (int i = 0; i <= count; i++) {
                stringList[i] = String.valueOf(Condition.split(":", count + 1)[i]);
            }
            String randomString = faker.get(key).options().option(stringList);
            Report.updateTestLog(Action, "Generated data: " + randomString, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, randomString);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating string: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate next element from an array of strings", input = InputType.YES, condition = InputType.YES)
    public void nextElementStringArray() {
        try {
            String strObj = Input;
            char splitChar = ':';
            int count = (int) Condition.chars().filter(ch -> ch == splitChar).count();
            String[] stringList = new String[count + 1];
            for (int i = 0; i <= count; i++) {
                stringList[i] = String.valueOf(Condition.split(":", count + 1)[i]);
            }
            String element = faker.get(key).options().nextElement(stringList);
            Report.updateTestLog(Action, "Generated data: " + element, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, element);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating element from string array: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    // Generate a random element from an enum
    public void nextElementEnum() {
        try {
            Day[] days = Day.values();
            Day element = faker.get(key).options().nextElement(days);
            Report.updateTestLog(Action, "Generated data: " + element, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during element generation from enum", ex);
            Report.updateTestLog(Action, "Error generating element from enum: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    // Enum for demonstration
    public enum Day {
        MONDAY, TUESDAY, WEDNESDAY, THURSDAY, FRIDAY, SATURDAY, SUNDAY
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Overwatch hero", input = InputType.YES, condition = InputType.NO)
    public void heroOverWatch() {
        try {
            String strObj = Input;
            String hero = faker.get(key).overwatch().hero();
            Report.updateTestLog(Action, "Generated data: " + hero, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hero);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Overwatch hero: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Overwatch location", input = InputType.YES, condition = InputType.NO)
    public void locationOverWatch() {
        try {
            String strObj = Input;
            String location = faker.get(key).overwatch().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Overwatch location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Overwatch quote", input = InputType.YES, condition = InputType.NO)
    public void quoteOverWatch() {
        try {
            String strObj = Input;
            String quote = faker.get(key).overwatch().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Overwatch quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random phone number", input = InputType.YES, condition = InputType.NO)
    public void phoneNumber() {
        try {
            String strObj = Input;
            String phoneNumber = faker.get(key).phoneNumber().phoneNumber();
            Report.updateTestLog(Action, "Generated data: " + phoneNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, phoneNumber);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating phone number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random cell phone number", input = InputType.YES, condition = InputType.NO)
    public void cellPhone() {
        try {
            String strObj = Input;
            String phoneNumber = faker.get(key).phoneNumber().cellPhone();
            Report.updateTestLog(Action, "Generated data: " + phoneNumber, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, phoneNumber);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating phone number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Pokemon name", input = InputType.YES, condition = InputType.NO)
    public void namePokemon() {
        try {
            String strObj = Input;
            String pokemonName = faker.get(key).pokemon().name();
            Report.updateTestLog(Action, "Generated data: " + pokemonName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, pokemonName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Pokemon name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Pokemon location", input = InputType.YES, condition = InputType.NO)
    public void locationPokemon() {
        try {
            String strObj = Input;
            String pokemonLocation = faker.get(key).pokemon().location();
            Report.updateTestLog(Action, "Generated data: " + pokemonLocation, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, pokemonLocation);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Pokemon location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random hex", input = InputType.YES, condition = InputType.NO)
    public void randomHex() {
        try {
            String strObj = Input;
            String hex = faker.get(key).random().hex();
            Report.updateTestLog(Action, "Generated data: " + hex, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hex);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random integer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random hex with specified length", input = InputType.YES, condition = InputType.YES)
    public void randomHexWithLength() {
        try {
            String strObj = Input;
            String lengthStr = Condition;
            int length = Integer.parseInt(lengthStr);
            String hex = faker.get(key).random().hex(length);
            Report.updateTestLog(Action, "Generated data: " + hex, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hex);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random integer: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random integer with length", input = InputType.YES, condition = InputType.YES)
    public void randomNextIntWithLength() {
        try {
            String strObj = Input;
            String numStr = Condition;
            int num = Integer.parseInt(numStr);
            int randomInt = faker.get(key).random().nextInt(num);
            Report.updateTestLog(Action, "Generated data: " + randomInt, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Integer.toString(randomInt));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random integer in range: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random integer within a range", input = InputType.YES, condition = InputType.YES)
    public void randomNextIntInRange() {
        try {
            String strObj = Input;
            String minStr = Condition.split(":", 2)[0];
            String maxStr = Condition.split(":", 2)[1];
            int min = Integer.parseInt(minStr);
            int max = Integer.parseInt(maxStr);
            int randomInt = faker.get(key).random().nextInt(min, max);
            Report.updateTestLog(Action, "Generated data: " + randomInt, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Integer.toString(randomInt));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random integer in range: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random long number", input = InputType.YES, condition = InputType.NO)
    public void randomNextLong() {
        try {
            String strObj = Input;
            long randomLong = faker.get(key).random().nextLong();
            Report.updateTestLog(Action, "Generated data: " + randomLong, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomLong));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random long: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random long number with length", input = InputType.YES, condition = InputType.YES)
    public void randomNextLongWithLength() {
        try {
            String strObj = Input;
            String numStr = Condition;
            long num = Long.parseLong(numStr);
            long randomLong = faker.get(key).random().nextLong(num);
            Report.updateTestLog(Action, "Generated data: " + randomLong, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Long.toString(randomLong));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random long: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random boolean value", input = InputType.YES, condition = InputType.NO)
    public void randomNextBoolean() {
        try {
            String strObj = Input;
            boolean randomBoolean = faker.get(key).random().nextBoolean();
            Report.updateTestLog(Action, "Generated data: " + randomBoolean, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Boolean.toString(randomBoolean));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random boolean: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Double number", input = InputType.YES, condition = InputType.NO)
    public void randomNextDouble() {
        try {
            String strObj = Input;
            Double randomDouble = faker.get(key).random().nextDouble();
            Report.updateTestLog(Action, "Generated data: " + randomDouble, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, Double.toString(randomDouble));
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating random float: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Rick and Morty character", input = InputType.YES, condition = InputType.NO)
    public void characterRickAndMorty() {
        try {
            String strObj = Input;
            String character = faker.get(key).rickAndMorty().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Rick and Morty character: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Rick and Morty location", input = InputType.YES, condition = InputType.NO)
    public void locationRickAndMorty() {
        try {
            String strObj = Input;
            String location = faker.get(key).rickAndMorty().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Rick and Morty location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Rick and Morty quote", input = InputType.YES, condition = InputType.NO)
    public void quoteRickAndMorty() {
        try {
            String strObj = Input;
            String quote = faker.get(key).rickAndMorty().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Rick and Morty quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Robin quote", input = InputType.YES, condition = InputType.NO)
    public void quoteRobin() {
        try {
            String strObj = Input;
            String quote = faker.get(key).robin().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Rick and Morty quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random RockBand name", input = InputType.YES, condition = InputType.NO)
    public void nameRockBand() {
        try {
            String strObj = Input;
            String name = faker.get(key).rockBand().name();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Rick and Morty quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Hamlet quote", input = InputType.YES, condition = InputType.NO)
    public void quoteHamlet() {
        try {
            String strObj = Input;
            String hamletQuote = faker.get(key).shakespeare().hamletQuote();
            Report.updateTestLog(Action, "Generated data: " + hamletQuote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, hamletQuote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Hamlet quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random As You Like It quote", input = InputType.YES, condition = InputType.NO)
    public void quoteAsYouLikeIt() {
        try {
            String strObj = Input;
            String asYouLikeItQuote = faker.get(key).shakespeare().asYouLikeItQuote();
            Report.updateTestLog(Action, "Generated data: " + asYouLikeItQuote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, asYouLikeItQuote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating As You Like It quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random King Richard III quote", input = InputType.YES, condition = InputType.NO)
    public void quoteKingRichard() {
        try {
            String strObj = Input;
            String kingRichardQuote = faker.get(key).shakespeare().kingRichardIIIQuote();
            Report.updateTestLog(Action, "Generated data: " + kingRichardQuote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, kingRichardQuote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating King Richard III quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Romeo and Juliet quote", input = InputType.YES, condition = InputType.NO)
    public void quoteRomeoAndJuliet() {
        try {
            String strObj = Input;
            String romeoAndJulietQuote = faker.get(key).shakespeare().romeoAndJulietQuote();
            Report.updateTestLog(Action, "Generated data: " + romeoAndJulietQuote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, romeoAndJulietQuote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Romeo and Juliet quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji", input = InputType.YES, condition = InputType.NO)
    public void slackEmoji() {
        try {
            String strObj = Input;
            String emoji = faker.get(key).slackEmoji().emoji();
            Report.updateTestLog(Action, "Generated data: " + emoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, emoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack People Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to people", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiPeople() {
        try {
            String strObj = Input;
            String peopleEmoji = faker.get(key).slackEmoji().people();
            Report.updateTestLog(Action, "Generated data: " + peopleEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, peopleEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack People Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to nature", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiNature() {
        try {
            String strObj = Input;
            String natureEmoji = faker.get(key).slackEmoji().nature();
            Report.updateTestLog(Action, "Generated data: " + natureEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, natureEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Nature Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to food and drink", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiFoodAndDrink() {
        try {
            String strObj = Input;
            String foodAndDrinkEmoji = faker.get(key).slackEmoji().foodAndDrink();
            Report.updateTestLog(Action, "Generated data: " + foodAndDrinkEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, foodAndDrinkEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Food and Drink Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to celebration", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiCelebration() {
        try {
            String strObj = Input;
            String celebrationEmoji = faker.get(key).slackEmoji().celebration();
            Report.updateTestLog(Action, "Generated data: " + celebrationEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, celebrationEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Celebration Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to activity", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiActivity() {
        try {
            String strObj = Input;
            String activityEmoji = faker.get(key).slackEmoji().activity();
            Report.updateTestLog(Action, "Generated data: " + activityEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, activityEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Activity Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to travel and places", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiTravelAndPlaces() {
        try {
            String strObj = Input;
            String travelAndPlacesEmoji = faker.get(key).slackEmoji().travelAndPlaces();
            Report.updateTestLog(Action, "Generated data: " + travelAndPlacesEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, travelAndPlacesEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Travel and Places Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to objects and symbols", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiObjectsAndSymbols() {
        try {
            String strObj = Input;
            String objectsAndSymbolsEmoji = faker.get(key).slackEmoji().objectsAndSymbols();
            Report.updateTestLog(Action, "Generated data: " + objectsAndSymbolsEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, objectsAndSymbolsEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Objects and Symbols Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Slack emoji related to custom emojis", input = InputType.YES, condition = InputType.NO)
    public void slackEmojiCustom() {
        try {
            String strObj = Input;
            String customEmoji = faker.get(key).slackEmoji().custom();
            Report.updateTestLog(Action, "Generated data: " + customEmoji, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, customEmoji);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Slack Custom Emoji: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random planet name", input = InputType.YES, condition = InputType.NO)
    public void planet() {
        try {
            String strObj = Input;
            String planet = faker.get(key).space().planet();
            Report.updateTestLog(Action, "Generated data: " + planet, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, planet);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating planet: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random moon name", input = InputType.YES, condition = InputType.NO)
    public void moon() {
        try {
            String strObj = Input;
            String moon = faker.get(key).space().moon();
            Report.updateTestLog(Action, "Generated data: " + moon, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, moon);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating moon: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random galaxy name", input = InputType.YES, condition = InputType.NO)
    public void galaxy() {
        try {
            String strObj = Input;
            String galaxy = faker.get(key).space().galaxy();
            Report.updateTestLog(Action, "Generated data: " + galaxy, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, galaxy);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating galaxy: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random nebula name", input = InputType.YES, condition = InputType.NO)
    public void nebula() {
        try {
            String strObj = Input;
            String nebula = faker.get(key).space().nebula();
            Report.updateTestLog(Action, "Generated data: " + nebula, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, nebula);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating nebula: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random star cluster name", input = InputType.YES, condition = InputType.NO)
    public void starCluster() {
        try {
            String strObj = Input;
            String starCluster = faker.get(key).space().starCluster();
            Report.updateTestLog(Action, "Generated data: " + starCluster, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, starCluster);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating star cluster: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random constellation name", input = InputType.YES, condition = InputType.NO)
    public void constellation() {
        try {
            String strObj = Input;
            String constellation = faker.get(key).space().constellation();
            Report.updateTestLog(Action, "Generated data: " + constellation, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, constellation);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating constellation: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random star name", input = InputType.YES, condition = InputType.NO)
    public void star() {
        try {
            String strObj = Input;
            String star = faker.get(key).space().star();
            Report.updateTestLog(Action, "Generated data: " + star, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, star);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating star: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random agency name", input = InputType.YES, condition = InputType.NO)
    public void spaceAgency() {
        try {
            String strObj = Input;
            String agency = faker.get(key).space().agency();
            Report.updateTestLog(Action, "Generated data: " + agency, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, agency);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating space agency: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random agency abbreviation", input = InputType.YES, condition = InputType.NO)
    public void spaceAgencyAbbreviation() {
        try {
            String strObj = Input;
            String agencyAbbreviation = faker.get(key).space().agencyAbbreviation();
            Report.updateTestLog(Action, "Generated data: " + agencyAbbreviation, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, agencyAbbreviation);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating space agency abbreviation: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random space company name", input = InputType.YES, condition = InputType.NO)
    public void spaceCompany() {
        try {
            String strObj = Input;
            String company = faker.get(key).space().company();
            Report.updateTestLog(Action, "Generated data: " + company, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, company);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating space company: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random distance measurement", input = InputType.YES, condition = InputType.NO)
    public void distanceMeasurement() {
        try {
            String strObj = Input;
            String distanceMeasurement = faker.get(key).space().distanceMeasurement();
            Report.updateTestLog(Action, "Generated data: " + distanceMeasurement, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, distanceMeasurement);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating distance measurement: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random meteorite name", input = InputType.YES, condition = InputType.NO)
    public void meteorite() {
        try {
            String strObj = Input;
            String meteorite = faker.get(key).space().meteorite();
            Report.updateTestLog(Action, "Generated data: " + meteorite, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, meteorite);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating meteorite: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random nasa space craft name", input = InputType.YES, condition = InputType.NO)
    public void nasaSpaceCraft() {
        try {
            String strObj = Input;
            String spaceCraft = faker.get(key).space().nasaSpaceCraft();
            Report.updateTestLog(Action, "Generated data: " + spaceCraft, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, spaceCraft);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating meteorite: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Star Trek character name", input = InputType.YES, condition = InputType.NO)
    public void characterStarTrek() {
        try {
            String strObj = Input;
            String character = faker.get(key).starTrek().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Star Trek character: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Star Trek location", input = InputType.YES, condition = InputType.NO)
    public void locationStarTrek() {
        try {
            String strObj = Input;
            String location = faker.get(key).starTrek().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Star Trek location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Star Trek species", input = InputType.YES, condition = InputType.NO)
    public void specieStarTrek() {
        try {
            String strObj = Input;
            String species = faker.get(key).starTrek().specie();
            Report.updateTestLog(Action, "Generated data: " + species, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, species);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Star Trek species: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Star Trek villain", input = InputType.YES, condition = InputType.NO)
    public void villainStarTrek() {
        try {
            String strObj = Input;
            String villain = faker.get(key).starTrek().villain();
            Report.updateTestLog(Action, "Generated data: " + villain, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, villain);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Star Trek villain: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random stock ticker symbol from the NYSE exchange", input = InputType.YES, condition = InputType.NO)
    public void nyseSymbol() {
        try {
            String strObj = Input;
            String nyse = faker.get(key).stock().nyseSymbol();
            Report.updateTestLog(Action, "Generated data: " + nyse, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, nyse);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating NYSE stock symbol: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random stock ticker symbol from the NSDQ exchange", input = InputType.YES, condition = InputType.NO)
    public void nsdqSymbol() {
        try {
            String strObj = Input;
            String nsdq = faker.get(key).stock().nsdqSymbol();
            Report.updateTestLog(Action, "Generated data: " + nsdq, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, nsdq);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating NYSE stock symbol: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random superhero name", input = InputType.YES, condition = InputType.NO)
    public void superheroName() {
        try {
            String strObj = Input;
            String name = faker.get(key).superhero().name();
            Report.updateTestLog(Action, "Generated data: " + name, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, name);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during superhero name generation", ex);
            Report.updateTestLog(Action, "Error generating superhero name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random superhero power", input = InputType.YES, condition = InputType.NO)
    public void superheroPower() {
        try {
            String strObj = Input;
            String power = faker.get(key).superhero().power();
            Report.updateTestLog(Action, "Generated data: " + power, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, power);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating superhero power: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random superhero prefix", input = InputType.YES, condition = InputType.NO)
    public void superheroPrefix() {
        try {
            String strObj = Input;
            String prefix = faker.get(key).superhero().prefix();
            Report.updateTestLog(Action, "Generated data: " + prefix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, prefix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating superhero prefix: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random superhero suffix", input = InputType.YES, condition = InputType.NO)
    public void superheroSuffix() {
        try {
            String strObj = Input;
            String suffix = faker.get(key).superhero().suffix();
            Report.updateTestLog(Action, "Generated data: " + suffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, suffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating superhero suffix: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random superhero descriptor", input = InputType.YES, condition = InputType.NO)
    public void superheroDescriptor() {
        try {
            String strObj = Input;
            String descriptor = faker.get(key).superhero().descriptor();
            Report.updateTestLog(Action, "Generated data: " + descriptor, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, descriptor);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating superhero descriptor: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random team name", input = InputType.YES, condition = InputType.NO)
    public void teamName() {
        try {
            String strObj = Input;
            String teamName = faker.get(key).team().name();
            Report.updateTestLog(Action, "Generated data: " + teamName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, teamName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating team name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random team sport", input = InputType.YES, condition = InputType.NO)
    public void teamSport() {
        try {
            String strObj = Input;
            String sport = faker.get(key).team().sport();
            Report.updateTestLog(Action, "Generated data: " + sport, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, sport);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating team sport: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random team state", input = InputType.YES, condition = InputType.NO)
    public void teamState() {
        try {
            String strObj = Input;
            String state = faker.get(key).team().state();
            Report.updateTestLog(Action, "Generated data: " + state, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, state);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating team mascot: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random team creature", input = InputType.YES, condition = InputType.NO)
    public void teamCreature() {
        try {
            String strObj = Input;
            String creature = faker.get(key).team().creature();
            Report.updateTestLog(Action, "Generated data: " + creature, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, creature);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating team member: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Twin Peaks character", input = InputType.YES, condition = InputType.NO)
    public void characterTwinPeaks() {
        try {
            String strObj = Input;
            String character = faker.get(key).twinPeaks().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Twin Peaks character: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Twin Peaks location", input = InputType.YES, condition = InputType.NO)
    public void locationTwinPeaks() {
        try {
            String strObj = Input;
            String location = faker.get(key).twinPeaks().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Twin Peaks location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Twin Peaks quote", input = InputType.YES, condition = InputType.NO)
    public void quoteTwinPeaks() {
        try {
            String strObj = Input;
            String quote = faker.get(key).twinPeaks().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Twin Peaks quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random university name", input = InputType.YES, condition = InputType.NO)
    public void universityName() {
        try {
            String strObj = Input;
            String universityName = faker.get(key).university().name();
            Report.updateTestLog(Action, "Generated data: " + universityName, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, universityName);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating university name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random university prefix", input = InputType.YES, condition = InputType.NO)
    public void universityPrefix() {
        try {
            String strObj = Input;
            String universityPrefix = faker.get(key).university().prefix();
            Report.updateTestLog(Action, "Generated data: " + universityPrefix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, universityPrefix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating university name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random university suffix", input = InputType.YES, condition = InputType.NO)
    public void universitySuffix() {
        try {
            String strObj = Input;
            String universitySuffix = faker.get(key).university().suffix();
            Report.updateTestLog(Action, "Generated data: " + universitySuffix, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, universitySuffix);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating university name: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random weather description", input = InputType.YES, condition = InputType.NO)
    public void weatherDescription() {
        try {
            String strObj = Input;
            String description = faker.get(key).weather().description();
            Report.updateTestLog(Action, "Generated data: " + description, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, description);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating weather description: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random temperature in Celsius", input = InputType.YES, condition = InputType.NO)
    public void temperatureCelsius() {
        try {
            String strObj = Input;
            String temperature = faker.get(key).weather().temperatureCelsius();
            Report.updateTestLog(Action, "Generated data: " + temperature, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, temperature);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating temperature: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random temperature in Fahrenheit", input = InputType.YES, condition = InputType.NO)
    public void temperatureFahrenheit() {
        try {
            String strObj = Input;
            String temperature = faker.get(key).weather().temperatureFahrenheit();
            Report.updateTestLog(Action, "Generated data: " + temperature, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, temperature);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating humidity: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random temperature in Celsius within a range", input = InputType.YES, condition = InputType.YES)
    public void temperatureCelsiusWithinRange() {
        try {
            String strObj = Input;
            String minTempStr = Condition.split(":", 2)[0];
            String maxTempStr = Condition.split(":", 3)[1];
            int minTemp = Integer.parseInt(minTempStr);
            int maxTemp = Integer.parseInt(maxTempStr);
            String temperature = faker.get(key).weather().temperatureCelsius(minTemp, maxTemp);
            Report.updateTestLog(Action, "Generated data: " + temperature, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, temperature);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating temperature: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random temperature in Fahrenheit within a range", input = InputType.YES, condition = InputType.YES)
    public void temperatureFahrenheitWithinRange() {
        try {
            String strObj = Input;
            String minTempStr = Condition.split(":", 2)[0];
            String maxTempStr = Condition.split(":", 3)[1];
            int minTemp = Integer.parseInt(minTempStr);
            int maxTemp = Integer.parseInt(maxTempStr);
            String temperature = faker.get(key).weather().temperatureFahrenheit(minTemp, maxTemp);
            Report.updateTestLog(Action, "Generated data: " + temperature, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, temperature);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating humidity: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Witcher character", input = InputType.YES, condition = InputType.NO)
    public void characterWitcher() {
        try {
            String strObj = Input;
            String character = faker.get(key).witcher().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher character: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Witcher location", input = InputType.YES, condition = InputType.NO)
    public void locationWitcher() {
        try {
            String strObj = Input;
            String location = faker.get(key).witcher().location();
            Report.updateTestLog(Action, "Generated data: " + location, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, location);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher location: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Witcher quote", input = InputType.YES, condition = InputType.NO)
    public void quoteWitcher() {
        try {
            String strObj = Input;
            String quote = faker.get(key).witcher().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Witcher", input = InputType.YES, condition = InputType.NO)
    public void witcher() {
        try {
            String strObj = Input;
            String item = faker.get(key).witcher().witcher();
            Report.updateTestLog(Action, "Generated data: " + item, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, item);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher item: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Witcher monster", input = InputType.YES, condition = InputType.NO)
    public void monsterWitcher() {
        try {
            String strObj = Input;
            String monster = faker.get(key).witcher().monster();
            Report.updateTestLog(Action, "Generated data: " + monster, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, monster);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher item: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Witcher school", input = InputType.YES, condition = InputType.NO)
    public void schoolWitcher() {
        try {
            String strObj = Input;
            String school = faker.get(key).witcher().school();
            Report.updateTestLog(Action, "Generated data: " + school, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, school);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher item: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random yoda Quote", input = InputType.YES, condition = InputType.NO)
    public void quoteYoda() {
        try {
            String strObj = Input;
            String quote = faker.get(key).yoda().quote();
            Report.updateTestLog(Action, "Generated data: " + quote, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, quote);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Witcher item: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Zelda character", input = InputType.YES, condition = InputType.NO)
    public void characterZelda() {
        try {
            String strObj = Input;
            String character = faker.get(key).zelda().character();
            Report.updateTestLog(Action, "Generated data: " + character, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, character);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Zelda quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random Zelda game", input = InputType.YES, condition = InputType.NO)
    public void gameZelda() {
        try {
            String strObj = Input;
            String game = faker.get(key).zelda().game();
            Report.updateTestLog(Action, "Generated data: " + game, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, game);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating Zelda quote: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input string", input = InputType.YES, condition = InputType.YES)
    public void expression() {
        try {
            String strObj = Input;
            String expression = Condition;
            String evaluated = faker.get(key).expression(expression);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input pattern", input = InputType.YES, condition = InputType.YES)
    public void bothify() {
        try {
            String strObj = Input;
            String pattern = Condition;
            String evaluated = faker.get(key).bothify(pattern);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input pattern", input = InputType.YES, condition = InputType.YES)
    public void bothifyWithUpper() {
        try {
            String strObj = Input;
            String pattern = Condition;
            boolean isUpper = true;
            String evaluated = faker.get(key).bothify(pattern, isUpper);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input pattern", input = InputType.YES, condition = InputType.YES)
    public void letterify() {
        try {
            String strObj = Input;
            String pattern = Condition;
            String evaluated = faker.get(key).letterify(pattern);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input pattern", input = InputType.YES, condition = InputType.YES)
    public void letterifyWithUpper() {
        try {
            String strObj = Input;
            String pattern = Condition;
            boolean isUpper = true;
            String evaluated = faker.get(key).letterify(pattern, isUpper);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input pattern", input = InputType.YES, condition = InputType.YES)
    public void numerify() {
        try {
            String strObj = Input;
            String pattern = Condition;
            String evaluated = faker.get(key).numerify(pattern);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @Action(object = ObjectType.DATA, desc = "Generate a random expression based on input pattern", input = InputType.YES, condition = InputType.YES)
    public void regexify() {
        try {
            String strObj = Input;
            String pattern = Condition;
            String evaluated = faker.get(key).regexify(pattern);
            Report.updateTestLog(Action, "Generated data: " + evaluated, Status.DONE);
            String sheetName = strObj.split(":", 2)[0];
            String columnName = strObj.split(":", 2)[1];
            userData.putData(sheetName, columnName, evaluated);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during data generation", ex);
            Report.updateTestLog(Action, "Error generating data: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }
}
