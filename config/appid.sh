# grep -rl '#{current_application_id}' app/src | xargs sed -i '' 's/#{current_application_id}/#{Settings.application_id}/g'
company_folder='app/src/main/java/com/intfocus'
company_prefix='com.intfocus'

current_company_name=$(ls ${company_folder} | head -n 1)
[[ -z "${current_company_name}" ]] && echo 'cannot find company name' && exit

current_company_domain=${company_prefix}.${current_company_name}
current_company_path=${company_folder}/${current_company_name}

new_company_name='yonghuitest'
new_company_path=${company_folder}/${new_company_name}
new_company_domain=${company_prefix}.${new_company_name}

# mv ${current_company_path} ${new_company_path}
# echo "mv ${current_company_path} ${new_company_path} $([[ $? -eq 0 ]] && echo 'successfully' || echo 'failed')"

grep -rl "${current_company_domain}" app/src #| xargs sed -i '' "s/${current_company_domain}/${new_company_domain}/g"
echo "sed -i ${current_company_domain} ${new_company_domain} $([[ $? -eq 0 ]] && echo 'successfully' || echo 'failed')"

