# HuBERT-based Speech Emotion Recognition # Tunisian Speech Emotion Recognition (SER) System

This repository contains a Speech Emotion Recognition system based on the HuBERT model, with three different fine-tuning strategies: full fine-tuning, QKV fine-tuning, and classifier-only fine-tuning.

## Project Structure

- `hubert_model.py`: Contains the HuBERT model architecture for SER
- `standard_data_processor.py`: Handles data preparation with standard train/val/test splits
- `data_augmentation.py`: Implements data augmentation techniques (pitch shifting, time stretching, noise addition)
- `train_hubert_standard.py`: Main training script
- `predict_emotion.py`: Script for running inference on audio files
- `run_hubert_standard.py`: Script for running the entire pipeline

## Installation

1. Clone this repository
2. Install the required dependencies:

```bash
pip install -r /Users/chihebnouri/SER/requirements.txt
```

## Training

The system supports three fine-tuning strategies:

### 1. Full Fine-tuning (Best Performance)

```bash
python /Users/chihebnouri/SER/train_hubert_standard.py \
    --data_dir /Users/chihebnouri/SER/tounsi \
    --output_dir /Users/chihebnouri/SER/results_hubert_standard/full \
    --fine_tuning_type full \
    --batch_size 8 \
    --epochs 20 \
    --learning_rate 1e-5
```

### 2. QKV Fine-tuning

```bash
python /Users/chihebnouri/SER/train_hubert_standard.py \
    --data_dir /Users/chihebnouri/SER/tounsi \
    --output_dir /Users/chihebnouri/SER/results_hubert_standard/qkv \
    --fine_tuning_type qkv \
    --batch_size 8 \
    --epochs 20 \
    --learning_rate 1e-5
```

### 3. Classifier-only Fine-tuning

```bash
python /Users/chihebnouri/SER/train_hubert_standard.py \
    --data_dir /Users/chihebnouri/SER/tounsi \
    --output_dir /Users/chihebnouri/SER/results_hubert_standard/classifier \
    --fine_tuning_type classifier \
    --batch_size 8 \
    --epochs 20 \
    --learning_rate 1e-5
```

### Advanced Training Options

You can customize various training parameters:

```bash
python /Users/chihebnouri/SER/train_hubert_standard.py \
    --data_dir /Users/chihebnouri/SER/tounsi \
    --output_dir /Users/chihebnouri/SER/results_hubert_standard/full \
    --fine_tuning_type full \
    --batch_size 16 \
    --epochs 30 \
    --learning_rate 2e-5 \
    --weight_decay 0.01 \
    --label_smoothing 0.1 \
    --hidden_size 256 \
    --test_size 0.2 \
    --val_size 0.1 \
    --seed 42
```

## Running Inference

After training, you can use the trained models to predict emotions from audio files.

### 1. Predict Emotion from a Single Audio File

```bash
python /Users/chihebnouri/SER/predict_emotion.py \
    --model_path /Users/chihebnouri/SER/results_hubert_standard/full/hubert_full_model.pt \
    --audio_file /path/to/your/audio_file.wav \
    --fine_tuning_type full
```

### 2. Batch Prediction on Multiple Audio Files

```bash
python /Users/chihebnouri/SER/predict_emotion.py \
    --model_path /Users/chihebnouri/SER/results_hubert_standard/full/hubert_full_model.pt \
    --audio_dir /path/to/your/audio_directory
```

## Web Application

The project includes a web application that provides a user-friendly interface for the Speech Emotion Recognition system. The application consists of three main components: Angular frontend, Spring Boot backend, and FastAPI for ML model inference.

### Prerequisites

- Java 11 or higher
- Maven
- Node.js and npm
- Angular CLI
- Python 3.8+ with pip
- Git

### Project Setup

1. Clone the repository (if not already done):

```bash
git clone <repository-url>
cd SER
```

2. Install Python dependencies:

```bash
pip install -r requirements.txt
```

### Running the FastAPI ML Service

The FastAPI service handles the machine learning inference for emotion recognition and phone detection:

```bash
# Navigate to the FastAPI directory
cd /Users/chihebnouri/SER/fastapi

# Start the FastAPI server
python -m uvicorn main:app --reload --host 0.0.0.0 --port 8000
```

The FastAPI service will be available at http://localhost:8000. You can access the interactive API documentation at http://localhost:8000/docs.

### Running the Spring Boot Backend

The Spring Boot backend serves as the middleware between the frontend and the ML services:

```bash
# Navigate to the backend directory
cd /Users/chihebnouri/SER/ser-web-app/backend

# Run the Spring Boot application using Maven
mvn spring-boot:run
```

The backend will start on http://localhost:8080.

### Running the Angular Frontend

The Angular frontend provides the user interface for the Interview Monitoring System:

```bash
# Navigate to the frontend directory
cd /Users/chihebnouri/SER/ser-web-app/frontend/ser-frontend

# Install dependencies (if not already installed)
npm install

# Start the Angular development server
ng serve
```

The frontend will be available at http://localhost:4200.

### Development Workflow

1. Start all three services in separate terminal windows (FastAPI, Spring Boot, and Angular)
2. Make changes to the respective components as needed
3. The services have hot-reload enabled, so most changes will be reflected automatically

### Building for Production

#### Angular Frontend

```bash
cd /Users/chihebnouri/SER/ser-web-app/frontend/ser-frontend
ng build --prod
```

The build artifacts will be stored in the `dist/` directory.

#### Spring Boot Backend

```bash
cd /Users/chihebnouri/SER/ser-web-app/backend
mvn clean package
```

This will generate a JAR file in the `target/` directory.

### Using the Web Application

1. Open your browser and navigate to http://localhost:4200
2. The Interview Monitoring System interface will be displayed
3. Grant camera and microphone permissions when prompted
4. The system will detect emotions from speech using the HuBERT model
5. The system will also detect phone usage during interviews using YOLO
6. Results are displayed in the tabbed interface on the right side

### Troubleshooting

- If you encounter CORS issues, ensure all three services are running and properly configured
- For webcam issues, check browser permissions and ensure no other application is using the camera
- If model inference is slow, consider reducing the processing frequency in the settings

## Prediction Options

### 3. Live Microphone Recording and Prediction

```bash
python /Users/chihebnouri/SER/predict_emotion.py \
    --model_path /Users/chihebnouri/SER/results_hubert_standard/full/hubert_full_model.pt \
    --live \
    --duration 5 \
    --fine_tuning_type full
```

## Performance Results

Based on our experiments:

- **Full Fine-tuning**: 50.98% accuracy, 0.5019 F1 score
- **QKV Fine-tuning**: 25.49% accuracy, 0.2113 F1 score
- **Classifier-only Fine-tuning**: Initially failed due to incorrect LoRA configuration

## Dataset

The Tunisian Speech Emotion Recognition dataset uses a specific naming convention for audio files: `speaker{id}_{emotion}_{number}.m4a`. The dataset includes the following emotions:

- happy
- fear
- surprise
- sadness
- neutral
- anger
- disgust

## Running the Complete Pipeline

To run the complete pipeline (data preparation, training, and evaluation):

```bash
python /Users/chihebnouri/SER/run_hubert_standard.py \
    --data_dir /Users/chihebnouri/SER/tounsi \
    --output_dir /Users/chihebnouri/SER/results_hubert_standard \
    --fine_tuning_type full
```

## Notes

- The system uses random stratified splits with 70% training, 10% validation, and 20% test data by default.
- Data augmentation (pitch shifting, time stretching, noise addition) is automatically applied during training.
- The full fine-tuning approach generally provides the best performance but requires more computational resources.
- For parameter-efficient fine-tuning, the QKV approach offers a good balance between performance and resource usage.
