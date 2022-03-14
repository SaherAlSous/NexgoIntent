package com.saher.nexgointent

import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Typeface
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.gson.Gson
import com.nexgo.oaf.apiv3.APIProxy
import com.nexgo.oaf.apiv3.device.printer.AlignEnum
import com.nexgo.oaf.apiv3.device.printer.OnPrintListener
import com.nexgo.oaf.apiv3.device.printer.Printer
import org.json.JSONException
import org.json.JSONObject


//TAG string used for Logcat output
private const val TAG = "IntentCallerApp"

class MainActivity : AppCompatActivity()  {

    // Buttons and text inputs from the layout
    private lateinit var intent_sale_go_button: Button
    private lateinit var intent_sale_amount_input: EditText
    private lateinit var intent_tip_amount_input: EditText

    // JSON Objects that will be packed into an Intent & sent to the Integrator
    private lateinit var Input: JSONObject

    private var pSaleType: String = ""
    private val pReceipt = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        Input = JSONObject()
        intent_sale_go_button = findViewById<View>(R.id.intent_sale_go_button) as Button

        intent_sale_go_button.setOnClickListener {
            Log.i("GoButtonClicked", "GO pressed...Building intent..")

            //Begin to build the CreditSale JSON message (returns true/false depending on result)
            if (buildCreditSaleJSONMessage()) {
                //CreditSale message params built successfully...

                //Build the main JSON, and pack the credit JSON into the request
                val intent = Intent().apply {
                    action = "android.intnet.action.display_navigationbar"

                    action = "android.intent.action.Integrator" //The listener name of Integrator

                    putExtra("Input", Input.toString()) //Pack the Input JSONObject into the intent
                }

                //check the activity is available to handle intent
                if (isActivityAvailable(intent)) {
                    //Activity for target intent exists on device (i.e. Integrator) - can continue to call startActivityForResult(..)
                    Log.d(TAG, "Activity for target intent exists on the device.")
                    startActivityForResult(intent, 1)
                } else {
                    //Activity for target intent does NOT exist on device - do not call startActivityForResult(..) else will crash
                    Log.e(
                        TAG,
                        "Error: Activity for target intent does not exist on the device."
                    )
                    Toast.makeText(
                        applicationContext,
                        "Activity for Intent not available!",
                        Toast.LENGTH_LONG
                    ).show()
                }
            } else {
                //Failed to successfully build the CreditSale JSON request
                Log.e("GoButtonClicked", "buildCreditSaleJSONMessage() failed!")
            }
        }

        //Initialize the 2 EditText objects used to get the input AMOUNTs from the user
        intent_sale_amount_input = findViewById<View>(R.id.intent_sale_amount_input) as EditText
        intent_tip_amount_input = findViewById<View>(R.id.intent_tip_amount_input) as EditText

    }

    /**
     * Builds the JSON Message that will be put in an Intent and sent to the Exadigm Integrator to
     * initiate a Credit Sale with our desired amount. If the JSON message is able to be built
     * without any issues, it will return true, otherwise false.
     * @return true if json message is able to be built correctly, otherwise returns false.
     */
    private fun buildCreditSaleJSONMessage(): Boolean  {
        //Initialize JSONObjects that will be packed into the Intent sent to Integrator.
        val input   = JSONObject()
        val action  = JSONObject()
        val payment = JSONObject()

        //Action Parameters
        val pSignature        =  true //Require on-screen signature
        val pManual           =  true //Allow manual/keyed entry (as opposed to SWIPE/EMV/TAP)
        val pProcessor        =  "NVL"
        val pReceipt          =  true //Print a receipt

        try {
            //Put the required additional parameters for a 'Credit Sale' into the Action JSONObject
            action.put("signature", pSignature)
            action.put("receipt", pReceipt)
            action.put("manual", pManual)
            action.put("processor", pProcessor)
        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error building [ACTION] JSON..", Toast.LENGTH_LONG).show()
            return false
        }

        //Payment Parameters
        pSaleType         =  "Sale" //'Sale' = Credit Sale
        if (intent_sale_amount_input.text.toString().equals("", true))
        {
            Log.e("buildSaleJSONMessage()", "Amount input was blank..")
            return false
        }
        //Get the Sale parameters (amounts $$) entered by the user
        val  pSaleAmount       =  intent_sale_amount_input.text.toString()
        val  pTipAmount        =  intent_tip_amount_input.text.toString()
        val  pCashbackAmount   =  "0.00"

        try {
            //Put the required payment fields for a 'Credit Sale' into the Payment JSONObject
            payment.put("type", pSaleType)
            payment.put("amount", pSaleAmount)
            payment.put("tip_amount", pTipAmount)
            payment.put("cash_back", pCashbackAmount)

        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error building [PAYMENT] JSON..", Toast.LENGTH_SHORT).show()
            return false
        }

        //Input Parameters (sent with Intent)
        try {
            //Pack the various JSONObject (Action, Payment, Host) Strings into the Intent to send to Integrator app
            input.put("action", action)
            input.put("payment", payment)
            //Input.put("host", Host) //20190312 Hayden -- Remove sending the Host Params b/c no longer needed. It is handled by Integrator.
            Log.d(TAG, "Params:$input")

        } catch (e: JSONException) {
            e.printStackTrace()
            Toast.makeText(applicationContext, "Error building [INPUT] JSON..", Toast.LENGTH_SHORT).show()
            return false
        }

        return true
    }

    /**
     * Checks to assure that the activity required by the intent exists on the device.
     *
     * If the activity does not exist on the device, it will crash the app. Thus we must check that
     * it exists before calling startActitivtyForResult(..) to prevent the crash scenario.
     *
     * If the activity does exist, then the intent can be called safely (assuming no other issues).
     *
     * @param intent the Intent that will be called in startActivityForResult(..) to call the Integrator application.
     * @return true if the activity requested by the intent exists, else returns false.
     */
    private fun isActivityAvailable(intent: Intent?): Boolean {
        val manager = applicationContext.packageManager
        val infos = manager.queryIntentActivities(intent!!, 0)
        return infos.size > 0
    }

    /**
     * This function is used to display text in an 'AlertDialog' displayed to the user.
     *
     *
     * In this sample application, it is used to display the transaction result message returned
     * from the Integrator.
     *
     * @param body - The text to be displayed in the AlertDialog.
     */
    private fun showAlertDialog(title: String?, body: String?, context: Context?) {
        AlertDialog.Builder(context)
            .setPositiveButton(android.R.string.yes,
                DialogInterface.OnClickListener { dialog, which -> })
            .setNegativeButton(android.R.string.no,
                DialogInterface.OnClickListener { dialog, which ->
                    // do nothing
                })
            .setTitle(title)
            .setMessage(body)
            .show()
    }

    /**
     * The onActivityResult(X,X,X) function is called after the Integrator Intent has run
     * and the result is returned to the IntentCaller application.
     *
     * This is the function where we can handle operations after an Integrator transaction has
     * finished, like getting the 'Signature' image, parsing a TransactionKey, or just for checking
     * the raw response from the Integrator (including the raw HOST RESPONSE)
     * @param requestCode
     * @param resultCode the resultCode returned from the activity called in startActivityForResult(..)
     * @param data the data returned from Integrator, containing results and other messages/objs
     */

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1) {
            if (resultCode == RESULT_OK) {
                val response = data?.getStringExtra("transdata")
                println("RESPONSE ====> $response")

                // Create gson object
                val gson = Gson()
                // Convert JSON string to Java objects
                val pResponse = gson.fromJson(response, ResponseParser::class.java)
                println("pResponse Object => $pResponse")

                // Retrieve Transaction ID
                val transID: String = pResponse.packetData.value!!.transactionID
                println("TransactionID=$transID")

                //Check if there is a signature in the response data and it is not null.
                if (data != null) {
                    if (data.hasExtra("signature") && data.getByteArrayExtra("signature") != null && String(
                            data.getByteArrayExtra("signature")!!
                        ).compareTo("", ignoreCase = true) != 0
                    ) {
                        //Initialize the DeviceEngine object used to retrieve the printer component.
                        val deviceEngine = APIProxy.getDeviceEngine(this@MainActivity)

                        //Retrieve the Printer from the deviceEngine object.
                        val printer: Printer = deviceEngine.printer

                        //Init the printer
                        printer.initPrinter()

                        //Set the typeface
                        printer.setTypeface(Typeface.DEFAULT)

                        //Set the spacing
                        printer.setLetterSpacing(5)

                        //Create bitmap object from signature byte array
                        val signatureBitmap = BitmapFactory.decodeByteArray(
                            data.getByteArrayExtra("signature"),
                            0,
                            data.getByteArrayExtra("signature")!!.size
                        )

                        // Print customer signature to the receipt
                        // If pReceipt is set to false, do not use the code inside the if statement
                        if (pReceipt) {
                            //Append the title above where the signature will print
                            printer.appendPrnStr("Customer Signature:", 34, AlignEnum.LEFT, true)

                            //Append the signature image to the printer queue
                            printer.appendImage(signatureBitmap, AlignEnum.CENTER)
                        }

                        //Begin the print process using the the values set from above. Use callback to check print result.
                        printer.startPrint(true,
                            OnPrintListener { retCode ->
                                runOnUiThread {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Print Result: $retCode",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    if (retCode == 0) Log.d(
                                        "Printer retcode",
                                        "Printer Return code = [0] " + Thread.currentThread().name
                                    ) else if (retCode != 0) Log.e(
                                        "Printer retCode",
                                        "Printer Error! Return code = [" + retCode + "] " + Thread.currentThread().name
                                    )
                                }
                            })
                    }
                }

                //Show response to the user
                showAlertDialog(
                    "Response:",
                    "" + response,
                    this@MainActivity
                )
            }
        }
    }
}