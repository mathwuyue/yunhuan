# TODO: remove file after process xslx file

import pandas as pd

ALLOWED_EXTENSIONS = {"xlsx", "xls"}
CASE_SEL_KEYWORDS = """select a.name,a.phone_num1,a.phone_num2,a.sex,a.height,a.weight,a.id_card,a.province,a.city,a.district,a.town,a.street,a.detail_address,a.job,a.disease,a.health_care,a.surface_part,a.surface_area,a.surface_type,a.metabolic_ulcer,a.dm_time,a.dm_used,a.dm_course,a.df_time,a.df_cause,a.df_part,a.operation_part,a.amputation_part,a.smoke,a.drink_years,a.drinking,to_char(a.start_time,'YYYY-MM-DD HH24:MI:SS'),a.before_day,a.cure_way,a.pathogenic_bacteria,a.hosipital,a.hosipital_address,a.doctor,a.department,a.in_day,a.cost,a.course,a.cure_outcome,a.operation_course,a.file,to_char(a.save_time,'YYYY-MM-DD HH24:MI:SS'),b.username,a.id,to_char(a.birthday,'YYYY-MM-DD'),a.case_source,a.edu,a.dc,a.wagner,a.year,a.bmi,a.hba1c,a.wbc,a.crp,a.procalcitonin,a.fpg,a.uric_acid,a.triglyceride,a.total_cholesterol,a.hdlc,a.ldlc,a.hemoglobin,a.alb,a.abi,a.cre,a.tg,a.platelet,a.neut"""
CASE_SEL_PAGE_KEYWORDS = CASE_SEL_KEYWORDS + ",count(*) over() as full_count"
CASE_INSERT_KEYWORDS = """ (name,phone_num1,phone_num2,sex,height,weight,
                    id_card,province,city,district,town,street,detail_address,job,disease,health_care,
                    surface_part,surface_area,surface_type,metabolic_ulcer,dm_time,dm_used,dm_course,df_time,
                    df_cause,df_part,operation_part,amputation_part,smoke,drink_years,drinking,start_time,
                    before_day,cure_way,pathogenic_bacteria,hosipital,doctor,department,in_day,
                    cost,course,cure_outcome,operation_course,save_user,case_source,birthday,edu,dc,wagner,year,bmi,hba1c,wbc,crp,procalcitonin,fpg,uric_acid,triglyceride,total_cholesterol,hdlc,ldlc,hemoglobin,alb,abi,cre,tg,platelet,neut) values (
                    '{name}','{phone_num1}','{phone_num2}','{sex}',{height},{weight},'{id_card}','{province}',
                    '{city}','{district}','{town}','{street}','{detail_address}','{job}','{disease}','{health_care}',
                    '{surface_part}','{surface_area}','{surface_type}','{metabolic_ulcer}','{dm_time}','{dm_used}',
                    '{dm_course}','{df_time}','{df_cause}','{df_part}','{operation_part}','{amputation_part}',
                    '{smoke}','{drink_years}','{drinking}','{start_time}','{before_day}','{cure_way}',
                    '{pathogenic_bacteria}',{hosipital},'{doctor}','{department}',
                    '{in_day}',{cost},'{course}','{cure_outcome}','{operation_course}','{save_user}','{case_source}','{birthday}','{edu}','{dc}','{wagner}',{year},{bmi},{HbA10},{wbc},{crp},{procalcitonin},{fpg},{uric_acid},{triglyceride},{total_cholesterol},{HDLC},{LDLC},{hemoglobin},{alb},{ABI},{cre},{tg},{platelet},{neut}
                )  returning id;"""


def handle_pagination(req):
    page = req.args.get("page")
    num_page = req.args.get("num_page")
    if not page:
        page = 0
    if not num_page:
        num_page = 10
    return int(num_page), int(page) * int(num_page)


def gen_case_insert_sql(tbl, data):
    # print('''insert into ''' + tbl + CASE_INSERT_KEYWORDS.format(**data))
    return """insert into """ + tbl + CASE_INSERT_KEYWORDS.format(**data)


def cal_bmi(height, weight):
    try:
        height = float(height)
        weight = float(weight)
        if height > 100:
            height = height / 100
        return weight / height / height
    except ValueError:
        return 0
    except ZeroDivisionError:
        return 0


def allowed_file(filename):
    return "." in filename and filename.rsplit(".", 1)[1].lower() in ALLOWED_EXTENSIONS


def handle_case_upload(fname, save_user, hosipital, tbl, cur):
    df = pd.read_excel(
        fname,
        converters={
            "身高": _convert_float,
            "体重": _convert_float,
            "年龄": _convert_year,
            "出生年月": _convert_date,
            "首次治疗时间": _convert_date,
            "身份证号": str,
        },
        na_values=["无"],
    )
    df = df.assign(录入人=save_user).assign(hosipital=hosipital).fillna(0)
    tuples = [tuple(x) for x in df.to_numpy() if x[0] != 0]
    values = [
        cur.mogrify("(" + "%s," * 61 + "%s)", tup).decode("utf-8") for tup in tuples
    ]
    cols = "name,sex,height,weight,bmi,phone_num1,phone_num2,id_card,year,birthday,edu,job,province,detail_address,disease,health_care,drink_years,smoke,surface_part,surface_area,metabolic_ulcer,dm_time,dm_used,dm_course,df_time,df_cause,df_part,Pathogenic_bacteria,dc,hba1c,wagner,wbc,platelet,neut,crp,procalcitonin,fpg,uric_acid,triglyceride,total_cholesterol,hdlc,ldlc,hemoglobin,cre,tg,alb,abi,cure_way,operation_course,operation_part,amputation_part,before_day,in_day,course,start_time,cost,cure_outcome,doctor,department,case_source,save_user,hosipital"
    query = "INSERT INTO %s(%s) VALUES " % (tbl, cols) + ",".join(values)
    cur.execute(query)
    return len(tuples), {"status": 1, "resp": "完成上传录入！"}


def _convert_year(year):
    try:
        return float(year)
    except ValueError:
        return float(year[:-1])


def _convert_float(height):
    try:
        return float(height)
    except ValueError:
        return 0


def _convert_date(date):
    if not date:
        return "1900-06-08"
    else:
        return date
