RAW_URL_PATH=https://raw.githubusercontent.com/Dogfalo/materialize/v1-dev/dist
OUTPUT_FOLDER=./koarl-backend/src/main/resources

wget -q -O $OUTPUT_FOLDER/materialize.min.css $RAW_URL_PATH/css/materialize.min.css
wget -q -O $OUTPUT_FOLDER/materialize.min.js $RAW_URL_PATH/js/materialize.min.js
wget -q -O $OUTPUT_FOLDER/jquery-2.1.1.min.js https://code.jquery.com/jquery-2.1.1.min.js
