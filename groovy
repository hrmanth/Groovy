pipeline {
  environment {
    registry = "vinod9782/inspired"
    registryCredential = 'dockerhub'
    dockerImage = ''
  }
  agent any
  tools {nodejs "node" }
  stages {
    stage('Cloning Git') {
      steps {
        git 'https://github.com/hrmanth/game-of-life.git'
      }
    }
    stage('Building image') {
      steps{
        script {
          dockerImage = docker.build registry + ":$BUILD_NUMBER"
        }
      }
    }
    stage('Deploy Image') {
      steps{
         script {
            docker.withRegistry( '', registryCredential ) {
            dockerImage.push()
          }
        }
      }
    }
    stage('Remove Unused docker image') {
      steps{
        sh "docker rmi $registry:$BUILD_NUMBER"
      }
    }
    stage('upload war to nexus'){
       steps{
            nexusArtifactUploader artifacts: [
                [
                    artifactId: 'Inspired',
                    classifier: '',
                    file: 'gameoflife-web/target/gameoflife.war',
                    type: 'war'
               ]
                   
           ], 
                   
           credentialsId: 'Nexus',
           groupId: 'Inspired',
           nexusUrl: '3.134.114.169:8081/nexus',
           nexusVersion: 'NEXUS2',
           protocol: 'http',
           repository: 'Inspired', 
           version: '$BUILD_ID'
           }
         }
    stage('build & sonarqube analyis'){
       steps{
        withSonarQubeEnv('sonar-6'){
         sh 'mvn clean package sonar:sonar'
        }
       }
    }
    stage('Quality Gate'){
      steps {
        timeout(time:1, unit: 'HOURS'){
         waitForQualityGate abortPipeline: true
        }
      }
    }  
      }         
    }
