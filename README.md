jenkinstool
===========
About jenkins'jobXML building project
组装jenkins流水线任务构建jobXML实现和pipeline script封装
# pipeline script 样本

pipeline {
    agent none
    stages {
        stage('代码资源') {
            agent any
            steps {
              checkout([$class: 'GitSCM', branches: [[name: '*/master']], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: 'f8064670-ad4a-4a42-92b0-9df16e5d43b5', url: 'git@168.1.15.107:cloud/cam.git']]])
            }
        }
        stage('maven构建') {
            agent { docker 
                {
                image 'maven:3.3.3'
                args '-v /root/.m2:/root/.m2 -u root'
                }
            }
            steps {
                sh 'mvn clean package -Dmaven.test.skip=true'
            }
        }
        stage('代码检查') {
            agent { docker 
                {
                image 'cmp-sonar-scanner'
                args '-e SONAR_HOST_URL=http://168.1.25.21:9000 -u root'
                }
            }
            steps {
                sh 'echo "sonar.projectKey=$JOB_NAME:$BUILD_NUMBER\nsonar.projectName=$JOB_NAME\nsonar.projectVersion=$BUILD_NUMBER\nsonar.sources=.\nsonar.exclusions=**/test/**,**/target/**\nsonar.java.binaries=target/classes\nsonar.language=java\nsonar.sourceEncoding=UTF-8\n">sonar-project.properties'
                sh 'echo "sonar.host.url=${SONAR_HOST_URL}">>/sonar-scanner/conf/sonar-scanner.properties'
                sh '/sonar-scanner/bin/sonar-scanner -Dsonar.projectBaseDir=.'
            }
        }
        stage('镜像构建') {
            agent any
            steps {
                withCredentials([usernamePassword(credentialsId: 'f58a898a-bd99-46be-9449-c152723fb438', passwordVariable: 'password', usernameVariable: 'username')]) {
                sh '''
                echo "FROM openjdk:8u212-jdk\nMAINTAINER wangsq@tongtech.com\nADD ./target/cam.war .\nEXPOSE 8808\nCMD /bin/bash">Dockerfile
                docker build -t registry.cn-beijing.aliyuncs.com/1apps/cam:$BUILD_NUMBER .
                docker login -u $username -p $password registry.cn-beijing.aliyuncs.com
                docker push registry.cn-beijing.aliyuncs.com/1apps/cam:$BUILD_NUMBER
                docker rmi -f registry.cn-beijing.aliyuncs.com/1apps/cam:$BUILD_NUMBER
                '''
                }
            }
        }
        stage('发布应用'){
            agent any
            steps{
               sh 'curl -X GET http://168.1.25.21:8080/cam/api/v1/products/51c0e9ec-f2d6-3b41-a4e1-08299b34f10a'
            }
        }
    }
}
# jenkins JobXml 样本
<?xml version='1.1' encoding='UTF-8'?>
<flow-definition plugin="workflow-job@2.35">
  <actions/>
  <description></description>
  <keepDependencies>false</keepDependencies>
  <properties>
    <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
      <triggers>
        <!-- 构建其他项目后触发 -->
        <jenkins.triggers.ReverseBuildTrigger>
          <spec></spec>
          <upstreamProjects>demo</upstreamProjects>
          <threshold>
            <name>SUCCESS</name>
            <ordinal>0</ordinal>
            <color>BLUE</color>
            <completeBuild>true</completeBuild>
          </threshold>
        </jenkins.triggers.ReverseBuildTrigger>
        <!-- 定时触发 -->
        <hudson.triggers.TimerTrigger>
          <spec>H H 1,15 1-11 *</spec>
        </hudson.triggers.TimerTrigger>
        <!-- gitHub提交触发 -->
        <com.cloudbees.jenkins.GitHubPushTrigger plugin="github@1.29.5">
          <spec></spec>
        </com.cloudbees.jenkins.GitHubPushTrigger>
          <!-- gitLab提交触发 -->
        <com.dabsquared.gitlabjenkins.GitLabPushTrigger plugin="gitlab-plugin@1.5.13">
          <spec></spec>
          <triggerOnPush>true</triggerOnPush>
          <triggerOnMergeRequest>true</triggerOnMergeRequest>
          <triggerOnPipelineEvent>false</triggerOnPipelineEvent>
          <triggerOnAcceptedMergeRequest>false</triggerOnAcceptedMergeRequest>
          <triggerOnClosedMergeRequest>false</triggerOnClosedMergeRequest>
          <triggerOnApprovedMergeRequest>true</triggerOnApprovedMergeRequest>
          <triggerOpenMergeRequestOnPush>never</triggerOpenMergeRequestOnPush>
          <triggerOnNoteRequest>true</triggerOnNoteRequest>
          <noteRegex>Jenkins please retry a build</noteRegex>
          <ciSkip>true</ciSkip>
          <skipWorkInProgressMergeRequest>true</skipWorkInProgressMergeRequest>
          <setBuildDescription>true</setBuildDescription>
          <branchFilterType>All</branchFilterType>
          <includeBranchesSpec></includeBranchesSpec>
          <excludeBranchesSpec></excludeBranchesSpec>
          <sourceBranchRegex></sourceBranchRegex>
          <targetBranchRegex></targetBranchRegex>
          <secretToken>{AQAAABAAAAAQB3Ugh+0yYIQ+NqpmmAfVRB/b5+jzMHEH6JcGllq1Ycg=}</secretToken>
          <pendingBuildName></pendingBuildName>
          <cancelPendingBuildsOnUpdate>false</cancelPendingBuildsOnUpdate>
        </com.dabsquared.gitlabjenkins.GitLabPushTrigger>
      </triggers>
    </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
  </properties>
  <definition class="org.jenkinsci.plugins.workflow.cps.CpsFlowDefinition" plugin="workflow-cps@2.74">
    <script></script>
    <sandbox>true</sandbox>
  </definition>
  <triggers/>
  <disabled>false</disabled>
</flow-definition>
