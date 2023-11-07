*** Settings ***
Library               RequestsLibrary

*** Test Cases ***

Quick Get A JSON Body Test
    ${response}=    GET  http://zhangchl007.github.io/
    Should Contain  ${response.text}  Thanos-query testing for the different openshift-clusters