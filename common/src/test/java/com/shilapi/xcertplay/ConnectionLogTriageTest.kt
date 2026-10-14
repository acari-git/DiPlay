package com.shilapi.xcertplay

import org.junit.Assert.assertEquals
import org.junit.Test

class ConnectionLogTriageTest {
    @Test fun recognizesUsbErrorsFromReportedAndroid9Failures() {
        assertEquals(
            listOf(ConnectionLogTriage.Symptom.USB_TRANSPORT),
            ConnectionLogTriage.analyze(sequenceOf(
                "IAP2 RX [wired] FAILED: Android could not queue USBMUX read request (api=28)",
                "wired bring-up failed: USBMUX write transferred -1 of 36 bytes",
                "AirPlay transport error: Android could not queue the NCM read request",
            )),
        )
    }

    @Test fun recognizesBluetoothSocketAndRfcommTimeout() {
        assertEquals(
            listOf(ConnectionLogTriage.Symptom.BLUETOOTH),
            ConnectionLogTriage.analyze(sequenceOf(
                "bt socket connect failed",
                "wireless RFCOMM connect timed out after 15000ms",
                "Bluetooth socket manager missing failureClass=IOException",
            )),
        )
    }

    @Test fun recognizesWirelessHandoffAndServiceResolutionTimeout() {
        assertEquals(
            listOf(ConnectionLogTriage.Symptom.WIRELESS_TIMEOUT),
            ConnectionLogTriage.analyze(sequenceOf(
                "Wireless CarPlay handoff timed out waiting for tunnel iAP2",
                "CarPlay control service resolution timed out",
            )),
        )
    }

    @Test fun mostRecentDistinctProblemsAreFirstAndResultIsBounded() {
        assertEquals(
            listOf(ConnectionLogTriage.Symptom.USB_TRANSPORT, ConnectionLogTriage.Symptom.BLUETOOTH),
            ConnectionLogTriage.analyze(sequenceOf(
                "wireless carplay handoff timed out",
                "bt socket connect failed",
                "invalid usbmux frame length 0",
                "invalid usbmux frame length 0",
            )),
        )
    }

    @Test fun noEvidenceIsNotMistakenForSuccessOrAProblem() {
        assertEquals(emptyList<ConnectionLogTriage.Symptom>(), ConnectionLogTriage.analyze(sequenceOf(
            "Session: active",
            "USB connection ready",
            "Bluetooth paired",
            "NCM read request queued successfully",
            "a navigation track failed volume set",
            "Saved note: USBMUX write transferred successfully",
        )))
    }

    @Test fun genericExceptionWordsAndUnrelatedTimeoutsDoNotTriggerHints() {
        assertEquals(emptyList<ConnectionLogTriage.Symptom>(), ConnectionLogTriage.analyze(sequenceOf(
            "wireless address waiting preference=IPv6 timeoutMs=45000",
            "codec timed out",
            "usb read pending",
            "Bluetooth device name unavailable",
        )))
    }
}
