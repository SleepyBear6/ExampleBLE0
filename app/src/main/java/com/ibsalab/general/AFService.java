package com.ibsalab.general;

import org.json.JSONException;
import org.json.JSONObject;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class AFService {
    public String id;
    public String serialNumber;
    public String patientName;
    public String sex;
    public String patientIDNo;
    public Date birthday;
    public Date startDate;
    public int duration;
    public String site;
    public int priority;

    public AFService(JSONObject jObject) throws JSONException, ParseException {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd");

        id = jObject.getString("id");
        serialNumber = jObject.getString("serial_number");
        patientName = jObject.getString("patient_name");
        sex = jObject.getString("sex");
        patientIDNo = jObject.getString("patient_IDNumber");

        String birthdayStr = jObject.getString("birthday");
        birthday = !birthdayStr.equals("") ? dateFormat.parse(birthdayStr) : null;

        String startDateStr = jObject.getString("start_date");
        startDate = dateFormat.parse(startDateStr);

        duration = jObject.getInt("duration");
        site = jObject.getString("site");
        priority = jObject.getInt("priority");
    }
}
