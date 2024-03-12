import matplotlib.pyplot as plt
import numpy as np
from matplotlib.lines import Line2D
from matplotlib.patches import Patch

def leggi_dati_da_file(nome_file):
    time = []
    value = []
    with open(nome_file, 'r') as file:
        next(file)
        for line in file:
            t, v = map(float, line.split())
            time.append(t)
            value.append(v)
    return np.array(time), np.array(value)

# Leggi i dati dal primo file
time1, value1 = leggi_dati_da_file('Availability_thresholds.txt')

# Leggi i dati dal secondo file
time2, value2 = leggi_dati_da_file('Availability_values.txt')

time3, value3 = leggi_dati_da_file('Availability_current_values.txt')

# Creazione del grafico con il primo set di dati
plt.plot(time1, value1, label='Threshold', color='red', linestyle='-', linewidth=0.8)

# Creazione del grafico con il secondo set di dati
plt.plot(time2, value2, label='Latest Values', color=(0, 0.7, 0), linestyle='-', linewidth=0.8)

plt.plot(time3, value3, label='Current Values', color='blue', linestyle='-', linewidth=0.8)

punti_intersezione = [(3.87, 0.95), (4.29, 0.95), (8.03, 0.95), (8.97, 0.95)]

for punto in punti_intersezione:
    plt.scatter(punto[0], punto[1], color='black', s=10)

# Aggiunta dell'etichetta "Intersection" una sola volta nella legenda
plt.scatter([], [], color='black', label='Intersection')

# Aggiunta di etichette e titoli
plt.xlabel('Time [min]')
plt.ylabel('Value [%]')

# Linea vertical tratteggiata
#plt.axvline(x=5, color='gray', linestyle='--', label='Adaptation point', linewidth=1)

# Aggiunta della legenda
plt.legend()

# Eliminazione dei bordi neri superiore e destro
plt.gca().spines['right'].set_visible(False)
plt.gca().spines['top'].set_visible(False)

# Mostra il grafico
plt.show()
