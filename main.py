from flask import Flask, jsonify

app = Flask(__name__)

@app.route("/")
def index():
    return jsonify("hello world"), 200

@app.route("/health")
def health():
    return jsonify("Health is fine"), 201

if __name__ == "__main__":
    app.run(debug=True, port=8080,host='0.0.0.0')


