## Nerf Glassware Demo ##

By Zack Freedman of Voidstar Lab.
This project and and all original assets contained here are released into the public domain.

Recommended materials:
- Arduino Fio v3
- RN-42 in XBee form factor
- LiPo cell or 3x AA battery pack
- 2x reed switches
- Neodymium magnets
- Wire and heatshrink
- Cyanoacrylate glue, such as Zap-a-Gap
- Velcro, hot glue, etc
- Clip-fed Nerf Elite blaster, such as Rampage

All materials except the Nerf blaster can be purchased from SparkFun.

Hardware:
1. Solder wires to reed switches and apply heatshrink.
2. Solder wires of one reed switch to GND and D15 on the Fio
3. Solder wires of other reed switch to GND and D16 on the Fio
4. Cock the blaster and unscrew the stock attachment point from the back of the blaster. The plunger should be protruding from the blaster.
5. Thoroughly wipe the end of the plunger to remove grease.
6. Use the cyanoacrylate glue to attach a neodymium magnet to the end of the plunger.
7. After the glue dries, stack another magnet or two on top of it. Make sure the stock attachment point still fits over the extended plunger!
8. On the magazine, there's a ridge that lines up with the bottom of the blaster when the magazine is inserted into the blaster. Use the glue to attach a magnet to the magazine as close to this ridge as possible.
9. After the glue dries, stack another magnet or two onto the magazine. Make sure it still fits in the blaster!
10. Screw the stock attachment point back into place. Glue the D16 reed switch on the end so the plunger magnet causes it to trip when the blaster is cocked.
11. Insert the magazine into the blaster. Glue the D15 reed switch to the magazine well so the magazine magnet causes it to trip when the magazine is inserted.
12. Insert the RN42 into its socket on the Fio.
	14. Download the Fio v3 Arduino Addon Files from https://github.com/sparkfun/SF32u4_boards/archive/master.zip. Unzip it. Install Arduino if necessary. Find your Arduino's Sketchbook folder. Open the folder named 'hardware' contained here, or create it if it doesn't exist. Move the addon files into this folder.
15. Restart Arduino.
16. Open the firmware. Select Tools → Boards → SparkFun Fio V3. Select the Fio's serial port - this port will appear on the list when the Fio is connected and disappear when it's disconnected. On a Mac, it will have the form /dev/tty.usbserialXXXXXXXX.
17. Uncomment line 11 so CONFIGURE_RN4X is defined.
18. Upload the firmware and ensure it works.
19. Comment line 11 and reupload the firmware.
20. Stick the Fio and battery to the blaster.

The Android code is designed for ADT (Eclipse) and requires the Glass Development Kit Sneak Peek. It compiles as of 3/14/2014.