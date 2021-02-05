package athena;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;

import org.apache.commons.io.IOUtils;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.json.JSONObject;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;

import com.aventstack.extentreports.ExtentReports;
import com.aventstack.extentreports.ExtentTest;
import com.aventstack.extentreports.Status;
import com.aventstack.extentreports.reporter.ExtentHtmlReporter;

public class AthenaApi {
	private static final Logger log = LoggerFactory.getLogger(AthenaApi.class);

	public static void main(String[] args) throws IOException {
		boolean auth = false;

		String configPath = Paths.get(System.getProperty("user.dir") + "/src/main/resources/config/config.properties")
				.toString();

		Properties prop = readPropertiesFile(configPath);

		String excelPath = Paths.get(System.getProperty("user.dir") + "/src/main/resources/testcase/Test_cases.xlsx")
				.toString();

		Date today = Calendar.getInstance().getTime();
		SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy-hh.mm.ss");
		String currdate = formatter.format(today);

		try {
			Path path = Paths.get("./reports");
			Files.createDirectories(path);
		} catch (IOException e) {

		}

		ExtentHtmlReporter reporter = new ExtentHtmlReporter("./reports/Test-report-".concat(currdate).concat(".html"));
		ExtentReports extent = new ExtentReports();
		extent.attachReporter(reporter);

		XSSFWorkbook workbook = new XSSFWorkbook(excelPath);
		XSSFSheet sheet = workbook.getSheet("Sheet1");
		int rows = sheet.getPhysicalNumberOfRows();

		log.info("************************* FRAMEWORK EXECUTION STARTED *************************");
		log.info("Reading Test Cases from Excel Sheet");

		for (int i = 1; i < rows; i++) {
			String TC = sheet.getRow(i).getCell(0).getStringCellValue();
			String SQL = sheet.getRow(i).getCell(1).getStringCellValue();

			log.info("\n");
			log.info("********** EXECUTING TEST CASE # " + TC + " **********");
			log.info("Calling Lambda Function for Data Validation");
			log.info("Executing Query");

			ExtentTest logger = extent.createTest("Test Case " + i + " : " + TC);

			String query_url = prop.getProperty("endpoint");

			JSONObject json = new JSONObject();
			json.put("db", prop.getProperty("databaseName"));
			json.put("sql", SQL);
			logger.log(Status.INFO, "Test Case Execution Started");
			logger.log(Status.INFO, "Execution started for Test Case # " + i);
			logger.log(Status.INFO, "Test Case Name: " + TC);
			logger.log(Status.INFO, "Generating Payload for TC: " + i);
			logger.log(Status.INFO, "Payload for TC: " + i + " : " + json);

			URL url = new URL(query_url);
			try {
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setConnectTimeout(50000);
			conn.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
			conn.setRequestProperty("X-Amz-Security-Token",prop.getProperty("token"));
			conn.setDoOutput(true);
			conn.setDoInput(true);
			conn.setRequestMethod("POST");
			OutputStream os = conn.getOutputStream();
			os.write(json.toString().getBytes("UTF-8"));
			os.close();
			
			logger.log(Status.INFO, "Waiting for Result");
			log.info("Waiting for Lambda Function's Response");

			InputStream in = new BufferedInputStream(conn.getInputStream());
			String result = IOUtils.toString(in);

			if (result.contains("Pass")) {
				log.info("No Rows Populated, The records in Source and Target table are matched : PASS");
			} else {
				log.info("Rows Populated, The records in Source and Target table are not matched : FAIL");
			}

			log.info("Test Case # " + TC + " is Executed Successfully");

			if (result.contains("Pass")) {
				logger.log(Status.PASS, "No Rows Populated, The records in Source and Target table are matched : PASS");
			} else {
				logger.log(Status.FAIL,
						"Rows Populated, The records in Source and Target table are not matched : FAIL");
			}

			logger.log(Status.INFO, "Sucessfully Completed The Testing");

			extent.flush();
			in.close();
			conn.disconnect();
			workbook.close();
			auth=true;
			}
			catch(Exception e){
				log.info("Authentication Failed");
				auth=false;
			}
		}
		if(auth==true) {
			log.info("\n");
			log.info("Successfully Completed the Testing!");
			log.info("\n");
			log.info("Report is Successfully Generated");
			log.info("\n");
		}
		else {
			log.info("\n");
			log.info("Could not complete testing because of failed authentication");
		}
		log.info("************************ FRAMEWORK EXECUTION ENDED ***********************");
	}

	public static Properties readPropertiesFile(String fileName) throws IOException {
		FileInputStream fis = null;
		Properties prop = null;
		try {
			fis = new FileInputStream(fileName);
			prop = new Properties();
			prop.load(fis);
		} catch (FileNotFoundException fnfe) {
			fnfe.printStackTrace();
		} catch (IOException ioe) {
			ioe.printStackTrace();
		} finally {
			fis.close();
		}
		return prop;
	}
}
