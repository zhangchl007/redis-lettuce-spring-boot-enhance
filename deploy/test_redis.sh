if [ -z $1 ];then
   echo "Please input $0 ip"
   exit 1
else

while :;
do
	sleep 0.1
	curl -X POST -d @- -H 'Content-Type: application/json' \
http://$1:8080/api/v1/otp/generate <<'EOF'
{
	"email": "user2@gmail.com"
}
EOF

done
fi
