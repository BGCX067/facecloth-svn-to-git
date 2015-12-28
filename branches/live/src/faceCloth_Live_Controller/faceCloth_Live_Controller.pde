/***********************************
 * faceCloth_Live_Controller
 * 
 * @author Elie Zananiri
 * @email ez@prisonerjohn.com
 * @version 1.0
 *
 * @created 2006.09.02
 * @modified 2006.09.06
***********************************/
 

// initialize the DIGITAL pins
int aPin = 5;    // Handle 'A' Lock (orange) 
int bPin = 4;    // Handle 'B' Lock (green)
int cPin = 3;    // Handle 'C' Lock (blue) 
int dPin = 2;    // Handle 'D' Lock (red)

int hPin = 6;    // Draw 'H'andles Toggle
int ePin = 7;    // 'E'rase Frame Toggle
int sPin = 8;    // Draw 'S'creen Toggle
int oPin = 9;    // Draw 'O'utlines Toggle
int gPin = 10;    // Draw 'G'rid Toggle

int nAPin = 11;  // Handle 'A' Selected
int nBPin = 12;  // Handle 'B' Selected
int nCPin = 13;  // Handle 'C' Selected

// initialize the ANALOG pins
int xPin = 1;    // Handle 'X' Acceleration
int yPin = 0;    // Handle 'Y' Acceleration

int fPin = 2;    // Gravity 'F'orce Value

int rPin = 3;    // 'R'eset All


void setup() {
  // set all pin switches as INPUTs
  pinMode(aPin, INPUT);
  pinMode(bPin, INPUT);
  pinMode(cPin, INPUT);
  pinMode(dPin, INPUT);
  
  pinMode(hPin, INPUT);
  pinMode(ePin, INPUT);
  pinMode(sPin, INPUT);
  pinMode(oPin, INPUT);
  pinMode(gPin, INPUT);
  
  pinMode(nAPin, INPUT);
  pinMode(nBPin, INPUT);
  pinMode(nCPin, INPUT);
  
  // open the serial port
  Serial.begin(19200);
}


void loop() {
  // get handle lock values
  Serial.print(digitalRead(aPin));
  Serial.print(digitalRead(bPin));
  Serial.print(digitalRead(cPin));
  Serial.print(digitalRead(dPin));
  
  // get display option values
  Serial.print(digitalRead(hPin));
  Serial.print(digitalRead(ePin));
  Serial.print(digitalRead(sPin));
  Serial.print(digitalRead(oPin));
  Serial.print(digitalRead(gPin));
  
  // get handle selection value
  if (digitalRead(nAPin) == LOW) {
    Serial.print(1);
  } else if (digitalRead(nBPin) == LOW) {
    Serial.print(2);
  } else if (digitalRead(nCPin) == LOW) {
    Serial.print(3);
  } else {
    Serial.print(4);
  }
  
  // get reset value
  Serial.print(analogRead(rPin)/1023);
  delay(5);
  
  // get handle coordinate values
  Serial.print(treatValue(analogRead(xPin)));      
  delay(5);
  Serial.print(treatValue(analogRead(yPin)));      
  delay(5);
  
  // get gravity force value
  Serial.print(treatValue(analogRead(fPin)));      
  delay(5);
  
  // line feed
  Serial.println();
  delay(10);
}


int treatValue(int val) {
  // convert value from [0, 1023] to [0, 9]
  return ((val*9)/1023);
}
