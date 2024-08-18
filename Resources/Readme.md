# INGenious Playwright Studio - Test Automation for Everyone

## **Getting Started**

### **Prerequisites**
-------
**Hardware Requirements**

* RAM: Min. 2GB (preferably 4GB)
* Operating System: Windows (32/64 bit)/ MAC OS/Linux


> The framework is built using Java. Hence it will work on any Operating System which supports Java


**Software Requirements**

* Java 11 or above
* Maven [Installation guide can be found [here](https://maven.apache.org/install.html)]
* For customizations, any IDE which supports Java Development (eg. Eclipse, Netbeans, IntelliJ etc.)


**Launch**

* `Windows`
  
  Double click on the [`Run.bat`](#) in the framework location

* `Mac or Ubuntu`

    1. Open Terminal in the installation location and then type 
    ```shell
    chmod +x Run.command
    ```
    2. Then double click on the [`Run.command`](#)
    3. If you see **It's Downloaded From Internet** warning then enter the following command in terminal: 
     ```shell
     xattr -d -r com.apple.quarantine "/path/to/the framework"
     ```



-----------------------

## **Quick Start with Recording** - **Playwright Recorder (CodeGen)**



> Make sure Maven is installed in the system. INGenious internally uses Playwright codegen.


### Steps for recording


* Launch **INGenious Playwright Studio**

* Click on the **Recorder** icon

Internally this will call the following `mvn` command :

  ```
  mvn exec:java -f engine/pom.xml -e -D exec.mainClass=com.microsoft.playwright.CLI -D exec.args=codegen
  ```

* A loader will show up while the playwright-recorder is being loaded



> If a new version of Playwright is available, this step will try to download that first. So the recorder can time out if the network speed is slow.
  **Pay attention to the logs!!**

* The **Playwright Inspector** will launch along with **chromium** browser

* Enter the URL of the Application Under Test (AUT) in the **chromium** browser and perform the actions you want to perform on the application

* You will see the steps getting recorded in the **Playwright Inspector**

* Once the recording is done, **save the steps in a `.txt` file**
  <br>
  Currently only `.txt` is supported. Going forward all formats : `.java`, `.cs`, `.py`, `.js` will be supported for import



### Import the recording


* From **INGenious Playwright Studio**, navigate to **Tools** > **Import Playwright Recording** > **Import Playwright Recording**.

* Locate the **.txt file** and click [OK].

* The file is immediately rendered as **Scenario** and **Test Case**. All the relevant **test steps** with all the **web objects** and **test data** are imported.

* All the objects are loaded in the **Object Repository**.

Before you begin, its important that you [Know the Framework](https://ing-bank.github.io/ingenious-doc/knowyourframework/)

 