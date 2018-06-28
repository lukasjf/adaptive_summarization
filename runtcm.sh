
for i in {4..6}
do
for j in 1350000 650000 270000 135000 27000
do
echo $j $i
java -jar target/adaptive_summarization-1.0-SNAPSHOT.jar $j $i /home/lukas/studium/thesis/code/data/citation/graph /home/lukas/studium/thesis/code/data/citation/query1/ /home/lukas/studium/thesis/code/data/citation/query033/ /home/lukas/studium/thesis/code/data/citation/query01/ /home/lukas/studium/thesis/code/data/citation/query0033/ /home/lukas/studium/thesis/code/data/citation/query001/
done
done
