# Process IANA port list -
# 
# a) save IANA's port-numbers file as <filename>; then
# b) run "cat <filename> | awk -f process_iana.awk" or
# c) awk -f process_iana.awk <filename 1> ... <filename n>
# 
# Outputs "tcp_port.list" and "udp_port.list".

# main
BEGIN{
# Look for duplicates. (A tcp/udp port is assigned to a single app;
# but an app can use more than one ports.) Also, distinguish between
# tcp and udp ports.
prev = -1
tcp_file = "tcp_port.list"
print "" > tcp_file
udp_file = "udp_port.list"
print "" > udp_file
}
# Narrow down the search: look for <number>/<tcp> or <number>/<udp>.
/[0-9]*\/["udp","tcp"]/ {
# Ignore all comments
if (substr($1,1,1) == "#") {next}
# print $0
# Look for
# <short description> <port number>/<tcp or udp> <long description>
a_file = tcp_file
for (i=1; i<=NF; i++) {
	# The benefit here is that <number>/<tcp> appears as a single string.
	# valid = substr($i, index($i,"/")+1, length($i))
	split($i, s, "/")
	valid = s[2]
	if (valid == "tcp" || valid == "udp") {
	if (valid == "udp") 
	{a_file = udp_file}
	# port = substr($i,0,match($i,"/"))
	port = s[1]
	if (match(port, "^[0-9]+$")) {
		isDuplicate = 0
		# Check for duplicates:
		if (valid == "tcp")
		{
			tcp_duplicate[port]++
			if (tcp_duplicate[port] > 1)
			{
printf("Warning: found duplicate TCP port. Previous entry was in line %d. Ignoring line %d:\n%s\n",
	tcp_prev_line[port], NR, $0)
isDuplicate = 1
			}
			tcp_prev_line[port]=NR
		}
		else if (valid == "udp") 
		{
			udp_duplicate[port]++
			if (udp_duplicate[port] > 1) 
			{
printf("Warning: found duplicate UDP port. Previous entry was in line %d. Ignoring line %d:\n%s\n",
	udp_prev_line[port], NR, $0)
isDuplicate = 1
			}
			udp_prev_line[port]=NR
		}
		if (isDuplicate == 0)
		{	# Process as new;
			if (i == 2) 
			{printf("%s\t%s\n", port, $1) >> a_file}
			else 
			{ # Use long description instead.
			long_descr = substr($0, match($0,"\/[tcp,udp]")+1+3)
			gsub(" ","_",long_descr)
			printf("%s\t%s\n", port, long_descr) >> a_file
			}
		}
	} else if (match(port, "^[0-9]+-[0-9]+$")) {
		# port number is a range.
		# start = substr(port, 0, index(port,"-"))
		# end = substr(port, index(port,"-")+1, length(port))
		split(port, p, "-")
		start = p[1]
		end = p[2]
		if (i == 2) 
		{	for (j=start; j<=end; j++)
			{
			isDuplicate = 0
			# Check for duplicates:
			if (valid == "tcp")
			{
				tcp_duplicate[j]++
				if (tcp_duplicate[j] > 1)
				{
printf("Warning: found duplicate TCP port. Previous entry was in line %d. Ignoring line %d:\n%s\n",
	tcp_prev_line[j], NR, $0)
isDuplicate = 1
				}
				tcp_prev_line[j]=NR
			}
			else if (valid == "udp") 
			{
				udp_duplicate[j]++
				if (udp_duplicate[j] > 1) 
				{
printf("Warning: found duplicate UDP port. Previous entry was in line %d. Ignoring line %d:\n%s\n", 
	udp_prev_line[j], NR, $0)
isDuplicate = 1
				}
				udp_prev_line[j]=NR
			}
			if (isDuplicate == 0) {printf("%s\t%s\n", j, $1) >> a_file}
			}
		}
		else 
		{ 	long_descr = substr($0, match($0, "\/[tcp,udp]")+1+3)
			gsub(" ","_", long_descr)
			for (j=start; j<=end; j++)
			{
			isDuplicate = 0
			# Check for duplicates:
			if (valid == "tcp")
			{
				tcp_duplicate[j]++
				if (tcp_duplicate[j] > 1)
				{
printf("Warning: found duplicate TCP port. Previous entry was in line %d. Ignoring line %d:\n%s\n",
	tcp_prev_line[j], NR, $0)
isDuplicate = 1
				}
				tcp_prev_line[j]=NR
			}
			else if (valid == "udp") 
			{
				udp_duplicate[j]++
				if (udp_duplicate[j] > 1) 
				{
printf("Warning: found duplicate UDP port. Previous entry was in line %d. Ignoring line %d:\n%s\n", 
	udp_prev_line[j], NR, $0)
isDuplicate = 1
				}
				udp_prev_line[j]=NR
			}
			if (isDuplicate == 0) {printf("%s\t%s\n", j, long_descr) >> a_file}
			}
		}
	}
	} # end if (tcp or udp)
} # end for
} # end of main._
END {
count = 0
for (i in tcp_duplicate)
	if (tcp_duplicate[i] > 1) {
		print "Duplicate TCP port:", i, "(appears", tcp_duplicate[i], "times)"
		count++
	}
for (i in udp_duplicate)
	if (udp_duplicate[i] > 1) {
		print "Duplicate UDP port:", i, "(appears", udp_duplicate[i], "times)"
		count++
	}
print count, "warnings in total."
}
