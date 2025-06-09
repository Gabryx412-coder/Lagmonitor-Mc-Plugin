# LagMonitor

## Descrizione

LagMonitor ti permette di monitorare le prestazioni del tuo server Minecraft. Questo plugin si basa su potenti strumenti come VisualVM e Java Mission Control, entrambi forniti da Oracle. LagMonitor ti offre la possibilità di utilizzare molte delle funzionalità di questi strumenti direttamente all'interno di Minecraft. Questo è particolarmente utile per chi gestisce server e non può accedere direttamente a questi tool esterni.

Inoltre, il plugin è progettato specificamente per Minecraft: puoi controllare il TPS (Ticks per secondo), il ping dei giocatori, i tempi di esecuzione del server e molto altro.

## Funzionalità

- Ping dei giocatori
- Verifica offline della versione di Java
- Controlli di sicurezza sui thread
- Molte informazioni dettagliate sul tuo setup hardware (disco, processore, ecc.) e sul sistema operativo
- Monitoraggio dell’uso della CPU
- Analisi dell’uso della RAM
- Accesso agli stacktrace dei thread in esecuzione
- Visualizzazione dei ticks per secondo con storico
- Monitoraggio delle prestazioni di sistema
- Grafici visivi direttamente in gioco
- Visualizzatore dei tempi di esecuzione in gioco
- Accesso alle variabili di ambiente Java (mbeans)
- Profili specifici per plugin
- Controllo delle operazioni bloccanti sul thread principale
- Creazione di Heap e Thread dump
- Creazione di dump con Java Flight Recorder da analizzare successivamente sul proprio computer
- Registrazione delle prestazioni del server in un database MySQL/MariaDB

## Requisiti

- Java 8 o superiore
- Spigot 1.8.8 o fork compatibile (es. Paper)

## Permessi

- `lagmonitor.*` — Accesso a tutte le funzionalità di LagMonitor
- `lagmonitor.commands.*` — Accesso a tutti i comandi

### Permessi dei comandi

- lagmonitor.command.ping
- lagmonitor.command.ping.other
- lagmonitor.command.stacktrace
- lagmonitor.command.thread
- lagmonitor.command.tps
- lagmonitor.command.mbean
- lagmonitor.command.system
- lagmonitor.command.environment
- lagmonitor.command.timing
- lagmonitor.command.monitor
- lagmonitor.command.graph
- lagmonitor.command.native
- lagmonitor.command.vm
- lagmonitor.command.network
- lagmonitor.command.tasks
- lagmonitor.command.heap
- lagmonitor.command.jfr

## Comandi

/ping - Mostra il tuo ping verso il server
/ping <giocatore> - Mostra il ping di un giocatore specifico
/stacktrace - Mostra lo stacktrace del thread corrente
/stacktrace <nomeThread> - Mostra lo stacktrace di un thread specifico
/thread - Elenca tutti i thread in esecuzione con il loro stato
/tpshistory - Mostra il TPS attuale
/mbean - Elenca tutti gli mbean disponibili (informazioni sull’ambiente Java, JMX)
/mbean <nomeMbean> - Elenca gli attributi di uno specifico mbean
/mbean <nomeMbean> <attributo> - Mostra il valore di un attributo specifico
/system - Fornisce informazioni generali relative al server Minecraft
/env - Fornisce informazioni generali sul sistema operativo
/timing - Mostra i tempi di esecuzione del server in gioco
/monitor [start|stop|paste] - Monitora l’uso della CPU dei metodi
/graph [heap|cpu|threads|classes] - Mostra grafici visivi sul server (attualmente solo uso heap)
/native - Mostra informazioni native del sistema operativo
/vm - Mostra informazioni specifiche della JVM, come garbage collector, caricamento classi e specifiche VM
/network - Mostra la configurazione delle interfacce di rete
/tasks - Informazioni sui task in esecuzione e in coda
/heap - Dump dell’heap della memoria attuale
/lagpage <next|prev|numeroPagina|save|all> - Comando di paginazione per le sessioni correnti
/jfr <start|stop|dump> - Gestisce le registrazioni Java Flight Recorder della JVM nativa, offrendo informazioni dettagliate su rete, file, heap e thread



## Versioni di sviluppo

Le versioni di sviluppo possono essere scaricate dal server di integrazione continua (CI) indicato, contenente le ultime modifiche al codice sorgente in vista del prossimo rilascio. Possono includere nuove funzionalità, correzioni di bug e altri cambiamenti rispetto all’ultima versione stabile.

Attenzione: queste build sono testate solo parzialmente, quindi potrebbero contenere nuovi bug e risultare meno stabili.

[https://ci.codemc.org/job/gabrycoder/job/LagMonitor/changes](https://ci.codemc.org/job/gabrycoder/job/LagMonitor/changes)

## Richieste di rete

Il plugin effettua richieste di rete a:

- https://paste.enginehub.org — per caricare gli output dei comandi di monitoraggio

## Build riproducibili

Questo progetto supporta build riproducibili per una maggiore sicurezza. In breve, significa che il codice sorgente corrisponde esattamente al file jar prodotto. Alcuni output possono variare a seconda del sistema operativo (es. fine riga), versione JDK e timestamp di build.

Puoi estrarre queste informazioni usando  
[build-info](https://github.com/apache/maven-studies/tree/maven-buildinfo-plugin).

Con la configurazione corretta per line endings e versione JDK, puoi specificare un timestamp personalizzato con il comando:

mvn clean install -Dproject.build.outputTimestamp=DATE





## Immagini

### Comando Heap
![heap command](https://i.imgur.com/AzDwYxq.png)

### Comando Timing
![timing command](https://i.imgur.com/wAxnIxt.png)

### Grafico CPU (blu=processo, giallo=sistema) — Carico processo
![cpu graph](https://i.imgur.com/DajnZmP.png)

### Stacktrace e comandi Threads
![stacktrace and threads](https://i.imgur.com/XY7r9wz.png)

### Comando Ping
![ping command](https://i.imgur.com/LITJKWw.png)

### Thread Sampler (Comando Monitor)
![thread sample](https://i.imgur.com/OXOakN6.png)

### Comando System
![system command](https://i.imgur.com/hrIV6bW.png)

### Comando Environment
![environment command](https://i.imgur.com/gQwr126.png)

### Grafico utilizzo Heap (giallo=allocato, blu=usato)
![heap usage map](https://i.imgur.com/Yiz9h6G.png)
