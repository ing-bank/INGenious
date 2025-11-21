package com.ing.engine.commands.bankingData;

import com.ing.engine.commands.browser.Command;
import com.ing.engine.commands.browser.General;
import com.ing.engine.core.CommandControl;
import com.ing.engine.support.Status;
import com.ing.engine.support.methodInf.InputType;
import com.ing.engine.support.methodInf.ObjectType;
import com.jcraft.jsch.JSchException;
import org.iban4j.CountryCode;
import org.iban4j.Iban;
import org.iban4j.IbanUtil;

import java.util.Random;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IbanOperations extends General {

    public IbanOperations(CommandControl cc) {
        super(cc);
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Country Code", input = InputType.YES, condition = InputType.NO)
    public void setCountryCode() throws JSchException {
            try {
                CountryCode countryCode = CountryCode.valueOf(Data);
                ibanCountryCode.put(iterationContext, countryCode);
                Report.updateTestLog(Action, "Country Code " + countryCode + " " + countryCode.getName() +" has been set successfully", Status.DONE);
            } catch (Exception ex) {
                Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban country code", ex);
                Report.updateTestLog(Action, "Error in setting iban country code: " + "\n" + ex.getMessage(), Status.DEBUG);
            }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Account Number", input = InputType.YES, condition = InputType.NO)
    public void setAccountNumber() throws JSchException {
        try {
            ibanAccountNumber.put(iterationContext, Data);
            Report.updateTestLog(Action, "Country Code " + Data +"has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban account number", ex);
            Report.updateTestLog(Action, "Error in setting iban account number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Generate Random Account Number by Length", input = InputType.YES, condition = InputType.NO)
    public void setRandomAccountNumberByLength() {
        try {
            Random random = new Random();
            int length = Integer.parseInt(Data);
            StringBuilder sb = new StringBuilder(length);
            for (int i = 0; i < length; i++) {
                sb.append(random.nextInt(10)); // Append a random digit (0-9)
            }

            String accountNumber = sb.toString();
            ibanAccountNumber.put(iterationContext, accountNumber);

            Report.updateTestLog(Action, "Account Number " + accountNumber + " generated successfully by Length", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during Account Number Generation by Length", ex);
            Report.updateTestLog(Action, "Error in Account Number Generation by Length: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Branch Code", input = InputType.YES, condition = InputType.NO)
    public void setBranchCode() throws JSchException {
        try {
            ibanBranchCode.put(iterationContext, Data);
            Report.updateTestLog(Action, "Branch Code " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban branch code", ex);
            Report.updateTestLog(Action, "Error in setting iban branch code: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Bank Code", input = InputType.YES, condition = InputType.NO)
    public void setBankCode() throws JSchException {
        try {
            ibanBankCode.put(iterationContext, Data);
            Report.updateTestLog(Action, "Bank Code " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban bank code", ex);
            Report.updateTestLog(Action, "Error in setting iban bank code: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Bank Code Ext", input = InputType.YES, condition = InputType.NO)
    public void setBankCodeExt() throws JSchException {
        try {
            ibanBankCodeExt.put(iterationContext, Data);
            Report.updateTestLog(Action, "Bank Code Ext " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban bank code ext", ex);
            Report.updateTestLog(Action, "Error in setting iban bank code ext: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set National Check Digit", input = InputType.YES, condition = InputType.NO)
    public void setNationalCheckDigit() throws JSchException {
        try {
            ibanNationalCheckDigit.put(iterationContext, Data);
            Report.updateTestLog(Action, "National Check Digit " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban national check digit", ex);
            Report.updateTestLog(Action, "Error in setting iban national check digit: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set BBAN", input = InputType.YES, condition = InputType.NO)
    public void setBban() throws JSchException {
        try {
            ibanBban.put(iterationContext, Data);
            Report.updateTestLog(Action, "BBAN " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban BBAN", ex);
            Report.updateTestLog(Action, "Error in setting iban BBAN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Identification Number", input = InputType.YES, condition = InputType.NO)
    public void setIdentificationNumber() throws JSchException {
        try {
            ibanIdentificationNumber.put(iterationContext, Data);
            Report.updateTestLog(Action, "Identification Number " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban Identification Number", ex);
            Report.updateTestLog(Action, "Error in setting iban Identification Number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Account Type", input = InputType.YES, condition = InputType.NO)
    public void setAccountType() throws JSchException {
        try {
            ibanAccountType.put(iterationContext, Data);
            Report.updateTestLog(Action, "Account Type " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban Account Type", ex);
            Report.updateTestLog(Action, "Error in setting iban Account Type: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Left Padding", input = InputType.YES, condition = InputType.NO)
    public void setLeftPadding() throws JSchException {
        try {
            ibanLeftPadding.put(iterationContext, Boolean.parseBoolean(Data));
            Report.updateTestLog(Action, "Left Padding " + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban Left Padding", ex);
            Report.updateTestLog(Action, "Error in setting iban Left Padding: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Set Owner Account Type", input = InputType.YES, condition = InputType.NO)
    public void setOwnerAccountType() throws JSchException {
        try {
            ibanOwnerAccountType.put(iterationContext, Data);
            Report.updateTestLog(Action, "Account Owner Type" + Data + " has been set successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during setting iban Account Owner Type", ex);
            Report.updateTestLog(Action, "Error in setting iban Account Owner Type: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }


    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Generate Random IBAN", input = InputType.NO, condition = InputType.YES)
    public void generateRandomIban() {
        try {

            Boolean leftPaddingValue = ibanLeftPadding.get(iterationContext);
            if (leftPaddingValue == null) {
                leftPaddingValue = false;
            }

            Iban iban = new Iban.Builder()
                    .accountNumber(ibanAccountNumber.get(iterationContext))
                    .accountType(ibanAccountType.get(iterationContext))
                    .bankCode(ibanBankCode.get(iterationContext))
                    .bankCodeExt(ibanBankCodeExt.get(iterationContext))
                    .branchCode(ibanBranchCode.get(iterationContext))
                    .countryCode(ibanCountryCode.get(iterationContext))
                    .identificationNumber(ibanIdentificationNumber.get(iterationContext))
                    .leftPadding(leftPaddingValue)
                    .nationalCheckDigit(ibanNationalCheckDigit.get(iterationContext))
                    .ownerAccountType(ibanOwnerAccountType.get(iterationContext))
                    .buildRandom();

            Command.iban.put(iterationContext, iban);

            if (!Condition.isEmpty()) {
                if (Condition.startsWith("%") && Condition.endsWith("%")) {
                    addVar(Condition, iban.toString());
                    Report.updateTestLog(Action, "random IBAN " + iban.toString() + " is stored in variable " + Condition, Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
                }
            }

            Report.updateTestLog(Action, "Random IBAN " + iban.toString() + " generated successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during random IBAN generation", ex);
            Report.updateTestLog(Action, "Error in generating random IBAN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Generate IBAN", input = InputType.NO, condition = InputType.YES)
    public void generateIban() {
        try {

            Boolean leftPaddingValue = ibanLeftPadding.get(iterationContext);
            if (leftPaddingValue == null) {
                leftPaddingValue = false;
            }

            Iban iban = new Iban.Builder()
                    .accountNumber(ibanAccountNumber.get(iterationContext))
                    .accountType(ibanAccountType.get(iterationContext))
                    .bankCode(ibanBankCode.get(iterationContext))
                    .bankCodeExt(ibanBankCodeExt.get(iterationContext))
                    .branchCode(ibanBranchCode.get(iterationContext))
                    .countryCode(ibanCountryCode.get(iterationContext))
                    .identificationNumber(ibanIdentificationNumber.get(iterationContext))
                    .leftPadding(leftPaddingValue)
                    .nationalCheckDigit(ibanNationalCheckDigit.get(iterationContext))
                    .ownerAccountType(ibanOwnerAccountType.get(iterationContext))
                    .build();

            Command.iban.put(iterationContext, iban);

            if (!Condition.isEmpty()) {
                if (Condition.startsWith("%") && Condition.endsWith("%")) {
                    addVar(Condition, iban.toString());
                    Report.updateTestLog(Action, "random IBAN " + iban.toString() + " is stored in variable " + Condition, Status.DONE);
                } else {
                    Report.updateTestLog(Action, "Invalid variable format", Status.DEBUG);
                }
            }

            Report.updateTestLog(Action, "Random IBAN " + iban.toString() + " generated successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during IBAN generation", ex);
            Report.updateTestLog(Action, "Error in generating random IBAN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Validate IBAN", input = InputType.NO, condition = InputType.NO)
    public void validateIban() {
        try {
            Iban iban = Command.iban.get(iterationContext);
            IbanUtil.validate(iban.toString());
            Report.updateTestLog(Action, "IBAN " + iban.toString() + " validated successfully", Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during IBAN validation", ex);
            Report.updateTestLog(Action, "Error in validating IBAN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Country Code", input = InputType.NO, condition = InputType.YES)
    public void getCountryCode() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getCountryCode().getAlpha2());
            Report.updateTestLog(Action, "Country Code " + iban.getCountryCode().getAlpha2() + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban country code", ex);
            Report.updateTestLog(Action, "Error in saving iban country code: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Account Number", input = InputType.NO, condition = InputType.YES)
    public void getAccountNumber() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getAccountNumber());
            Report.updateTestLog(Action, "Account Number " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban account number", ex);
            Report.updateTestLog(Action, "Error in saving iban account number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Branch Code", input = InputType.NO, condition = InputType.YES)
    public void getBranchCode() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getBranchCode());
            Report.updateTestLog(Action, "Branch Code " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban branch code", ex);
            Report.updateTestLog(Action, "Error in saving iban branch code: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Bank Code", input = InputType.NO, condition = InputType.YES)
    public void getBankCode() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getBankCode());
            Report.updateTestLog(Action, "Bank Code " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban bank code", ex);
            Report.updateTestLog(Action, "Error in saving iban bank code: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Bank Code Ext", input = InputType.NO, condition = InputType.YES)
    public void getBankCodeExt() throws JSchException {
        try {
            String bankCodeExt = ibanBankCodeExt.get(iterationContext);
            addVar(Condition, bankCodeExt);
            Report.updateTestLog(Action, "Bank Code Ext " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban bank code ext", ex);
            Report.updateTestLog(Action, "Error in saving iban bank code ext: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get National Check Digit", input = InputType.NO, condition = InputType.YES)
    public void getNationalCheckDigit() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getNationalCheckDigit());
            Report.updateTestLog(Action, "National Check Digit " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban national check digit", ex);
            Report.updateTestLog(Action, "Error in saving iban national check digit: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get BBAN", input = InputType.NO, condition = InputType.YES)
    public void getBban() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getBban());
            Report.updateTestLog(Action, "BBAN " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban BBAN", ex);
            Report.updateTestLog(Action, "Error in saving iban BBAN: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Identification Number", input = InputType.NO, condition = InputType.YES)
    public void getIdentificationNumber() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getIdentificationNumber());
            Report.updateTestLog(Action, "Identification Number " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban Identification Number", ex);
            Report.updateTestLog(Action, "Error in saving iban Identification Number: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Account Type", input = InputType.NO, condition = InputType.YES)
    public void getAccountType() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getAccountType());
            Report.updateTestLog(Action, "Account Type " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban Account Type", ex);
            Report.updateTestLog(Action, "Error in saving iban Account Type: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Left Padding", input = InputType.NO, condition = InputType.YES)
    public void getLeftPadding() throws JSchException {
        try {
            Boolean leftPadding = ibanLeftPadding.get(iterationContext);
            addVar(Condition, Boolean.toString(leftPadding));
            Report.updateTestLog(Action, "Left Padding " + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban Left Padding", ex);
            Report.updateTestLog(Action, "Error in saving iban Left Padding: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }

    @com.ing.engine.support.methodInf.Action(object = ObjectType.IBAN, desc = "Get Owner Account Type", input = InputType.NO, condition = InputType.YES)
    public void getOwnerAccountType() throws JSchException {
        try {
            Iban iban = Command.iban.get(iterationContext);
            addVar(Condition, iban.getOwnerAccountType());
            Report.updateTestLog(Action, "Owner Account Type" + Data + " saved successfully in " + Condition, Status.DONE);
        } catch (Exception ex) {
            Logger.getLogger(this.getClass().getName()).log(Level.SEVERE, "Exception during saving iban Owner Account Type", ex);
            Report.updateTestLog(Action, "Error in saving iban Owner Account Type: " + "\n" + ex.getMessage(), Status.DEBUG);
        }
    }











}
