Gruppenmitglieder: Florian Hintermeier, Markus Pöchleitner, Michail Massaros


1 Image Rotation:
Aufgabe vollständig gelöst. Der Wert rotation am Anfang steuert die Grad der Rotation.


2 Scan:
Der Scan wurde mit folgender Spezifikation getestet:

number of platforms: 2
platform 0 name: Intel(R) OpenCL
platform 0 vendor: Intel(R) Corporation
platform 0 version: OpenCL 2.0
platform 1 name: NVIDIA CUDA
platform 1 vendor: NVIDIA Corporation
platform 1 version: OpenCL 1.2 CUDA 8.0.0
select platform: 0
number of devices for platform 0: 2
select device: 0

Unser Program funktioniert bis zu einer Anzahl von 16384 Elementen. Es ist fähig Vielfache der
workgroup Größe zu behandeln.
Dafür muss die Anzahl der Gruppen analog zur Anzahl der Elementen erhöht werden.
Leider kommt es bei höherer Anzahl (höher als 16384 Elemente) zu einer org.jocl.CLException: CL_OUT_OF_RESOURCES.

Der Algorithmus ist langsamer als die sequentielle Version, allerdings geht viel Zeit im Aufbau verloren.

Es gibt eine separate Grafik die eine Gegenüberstellung der Algorithmen darstellt, um die Performanz zu
vergleichen. (welche grafik? ) kann man


3 Radix Sort