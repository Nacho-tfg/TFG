/*

Copyright (c) 2012-2014 RedBearLab

Permission is hereby granted, free of charge, to any person obtaining a copy of this software 
and associated documentation files (the "Software"), to deal in the Software without restriction, 
including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, 
and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, 
subject to the following conditions:
The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, 
INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR 
PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE 
FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, 
ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.

*/

#include "mbed.h"
#include "ble/services/iBeacon.h"

#define UART_TX_PIN     P0_9       //TXD
#define UART_RX_PIN     P0_11      //RXD

Serial my_serial_port(UART_TX_PIN, UART_RX_PIN);
BLE ble;
DigitalOut myled(P0_19);

volatile bool sendSignal= false;

void sendNotificationLeft(){
    my_serial_port.printf("a\n");
}

void sendNotificationArrived(){
    my_serial_port.printf("b\n");
}

void periodicCallback(void)
{
    if(sendSignal){
        sendNotificationLeft();
    }
    sendSignal = true;
    myled = !myled;
}

void scanCallback(const Gap::AdvertisementCallbackParams_t *params){
                     
    if(params->peerAddr[5]==0xEE && params->peerAddr[4]==0x85 && params->peerAddr[3]==0xFF
         && params->peerAddr[2]==0x91 && params->peerAddr[1]==0xA0 && params->peerAddr[0]==0x8A){
        if(params->rssi > -82){
            sendNotificationArrived();
        }else if(params->rssi < -88){
            sendNotificationLeft();
        } 
    }
    
}

int main(void)
{   
    Ticker ticker;
    ticker.attach(&periodicCallback, 3.0);
    ble.init();
    
    /* SpinWait for initialization to complete. This is necessary because the
     * BLE object is used in the main loop below. */
    while (!ble.hasInitialized()) { /* spin loop */ }
    
    ble.gap().setScanParams(1500 /* scan interval */, 100 /* scan window */);
    ble.gap().startScan(scanCallback);

    while (true) {
        ble.waitForEvent(); // allows or low power operation
    }
}
