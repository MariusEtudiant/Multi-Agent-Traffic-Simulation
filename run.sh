#!/bin/bash


# à faire avant: chmod +x ton_script.sh puis pour lancer ./run.sh

#définir les chemins
JAVA_FX="lib/javafx-sdk-24.0.1/lib"
TWEETY_LIB="lib/tweety"
CP="out:$TWEETY_LIB/*:."

#compilation
echo "Compilation des sources..."
find src -name "*.java" > sources.txt

javac --module-path "$JAVA_FX" --add-modules javafx.controls -cp "$TWEETY_LIB/*" -d out @sources.txt
COMPILATION_STATUS=$?

#vérification de la compilation
if [ $COMPILATION_STATUS -ne 0 ]; then
    echo "Erreur de compilation. Arrêt du script."
    rm -f sources.txt
    exit 1
fi

#exécution
echo "Lancement de l'application..."
java -Dfile.encoding=UTF-8 --module-path "$JAVA_FX" --add-modules javafx.controls -cp "$CP" org.example.gui.TrafficSimulatorApp
EXECUTION_STATUS=$?

#nettoyage
rm -f sources.txt

#vérification de l'exécution
if [ $EXECUTION_STATUS -ne 0 ]; then
    echo "L'application a rencontré une erreur lors de l'exécution."
else
    echo "Application terminée avec succès."
fi

#pause pour ne pas fermer la console directement
read -p "Appuyez sur Entrée pour quitter.."
