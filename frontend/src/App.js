import React, { useState } from "react";
import axios from "axios";
import "./App.css";

function App() {
  const [file, setFile] = useState(null);
  const [transcript, setTranscript] = useState("");
  const [loading, setLoading] = useState(false);

  const handleFileChange = (e) => {
    setFile(e.target.files[0]);
    setTranscript("");
  };

  const handleUpload = async () => {
    if (!file) {
      alert("Please select an audio file!");
      return;
    }

    const formData = new FormData();
    formData.append("file", file);

    setLoading(true);
    try {
      const response = await axios.post("http://localhost:8080/api/transcribe", formData, {
        headers: { "Content-Type": "multipart/form-data" },
      });

      // response.data should be the transcript text
      setTranscript(response.data);
    } catch (error) {
      console.error("Transcription failed:", error);
      setTranscript("❌ Transcription failed. Try again or check console.");
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="app-container">
      <h1>🎤 Audio Transcriber</h1>
      <input type="file" accept="audio/*" onChange={handleFileChange} />
      <button onClick={handleUpload} disabled={loading}>
        {loading ? "Transcribing..." : "Transcribe"}
      </button>

      {transcript && (
        <div className="transcript-box">
          <h3>📄 Transcription:</h3>
          <p>{transcript}</p>
        </div>
      )}
    </div>
  );
}

export default App;