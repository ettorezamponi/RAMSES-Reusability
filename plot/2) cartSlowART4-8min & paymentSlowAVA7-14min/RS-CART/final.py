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
time1, value1 = leggi_dati_da_file('AverageResponseTime_thresholds.txt')

# Leggi i dati dal secondo file
time2, value2 = leggi_dati_da_file('AverageResponseTime_values.txt')

time3, value3 = leggi_dati_da_file('AverageResponseTime_current_values.txt')

# Creazione del grafico con il primo set di dati
plt.plot(time1, value1, label='Threshold', color='red', linestyle='-', linewidth=.8)

# Creazione del grafico con il secondo set di dati
# Trova l'indice dove time1 supera 5

index_split = np.searchsorted(time2, 3.567)

# Creazione del grafico con il primo set di dati prima di time1 = 5
plt.plot(time2, value2, color=(0, 0.7, 0), linestyle='-', linewidth=0.8, label='Latest Values (before adaptation)')

# Cambia il colore solo per il secondo set di dati dopo time1 = 5
plt.plot(time2[index_split:], value2[index_split:], color=(0, 0.15, 0), linestyle='-', linewidth=0.8, label='Latest Values (after adaptation)')



#plt.plot(time3, value3, label='Current Values', color='blue', linestyle='-', linewidth=.5)

punti_intersezione = [(2.223, 50), (3.945, 50)]

for punto in punti_intersezione:
    plt.scatter(punto[0], punto[1], color='black', s=10)

# Aggiunta dell'etichetta "Intersection" una sola volta nella legenda
plt.scatter([], [], color='black', label='Intersection')

# Aggiunta di etichette e titoli
plt.xlabel('Time [min]')
plt.ylabel('Value [%]')

# Linea vertical tratteggiata
plt.axvline(x=3.567, color='gray', linestyle='--', label='Adaptation point', linewidth=1)

# Aggiunta della legenda
plt.legend()

# Eliminazione dei bordi neri superiore e destro
plt.gca().spines['right'].set_visible(False)
plt.gca().spines['top'].set_visible(False)

# Mostra il grafico
plt.show()
