$(document).ready(function validateFrom (argument) {
	$("#form").validate({
		rules:{
			name:{
				required:true,
				minlength:2,
				maxlength:10
			},
			IDNumber:{
				required:true,
				minlength:18,
				maxlength:18,
				idCardCheck:true
			},
			age:{
				required:true,
				range:[18,60],
				digits:true
			},
			companyName:{
				required:true,
				minlength:2,
				maxlength:20,
				stringCheck:true
			},
			companyAddr:{
				minlength:6,
				maxlength:50,
				stringCheck:true
			},
			companyPhone:{
				required:true,
				minlength:5,
				maxlength:13,
				telCheck:true
			},
			clerk:{
				minlength:2,
				maxlength:8,
				stringCheck:true
			},
			email:{
				minlength:9,
				maxlength:18,
				emailCheck:true
			},
			mobileNo:{
				required:true,
				minlength:11,
				maxlength:11,
				mobileCheck:true
			},
			SmsCheckCode:{
				required:true,
				maxlength:6,
				minlength:6,
				digits:true,
				remote:{
					url:"/newsManagePlatform/checkCode",
					type:"POST",
					data:{
						//默认会把当前机校验的内容提交过去
						mobileNo:function(){
							return $("#mobileNo").val();
						}
					}
				}
			}
		},
		messages:{
			name:{
				required:"必须填写用户名",
				minlength:"用户名最小为2位",
				maxlength:"用户名最大为10位"
			},
			IDNumber:{
				required:"身份证不能为空",
				minlength:"身份证长度应为18位",
				maxlength:"身份证长度应为18位"
			},
			age:{
				required:"年龄不能为空",
				range:"年龄必须在18-60之间",
				digits:"年龄必须是整数"
			},
			companyName:{
				required:"公司名为必填项",
				minlength:"公司名最小长度为2",
				maxlength:"公司名不能超过20位",
				stringCheck:"公司名称非法"
			},
			companyAddr:{
				minlength:"地址不能少于6位",
				maxlength:"地址长度不能大于50位",
				stringCheck:"非法的地址"
			},
			companyPhone:{
				required:"公司电话为必填项",
				minlength:"电话长度非法",
				maxlength:"电话长度非法",
				telCheck:"电话号码非法"
			},
			clerk:{
				minlength:"职务不能少于2个字符",
				maxlength:"职务不能多于8个字符",
				stringCheck:"职务名称非法"
			},
			email:{
				minlength:"email地址不能少于9位",
				maxlength:"email地址不能大于18位",
				emailCheck:"email地址非法"
			},
			mobileNo:{
				required:"手机号为必填项",
				minlength:"非法的手机号",
				maxlength:"非法的手机号",
				mobileCheck:"非法的手机号"
			},
			SmsCheckCode:{
				required:"验证码为必填项",
				maxlength:"验证码为6位纯数字",
				minlength:"验证码为6位纯数字",
				digits:"验证码为6位纯数字",
				remote:"验证码不正确"
			}
		}
	});
	//添加自定义验证的方法
	jQuery.validator.addMethod("idCardCheck",function(value,element,params){
		var Wi = [ 7, 9, 10, 5, 8, 4, 2, 1, 6, 3, 7, 9, 10, 5, 8, 4, 2, 1 ];    // 加权因子   
		var ValideCode = [ 1, 0, 10, 9, 8, 7, 6, 5, 4, 3, 2 ];            // 身份证验证位值.10代表X 
		var sum = 0;                             // 声明加权求和变量  
		var a_idCard = value.split(""); 
		    if (a_idCard[17].toLowerCase() == 'x') {   
		        a_idCard[17] = 10;                    // 将最后位为x的验证码替换为10方便后续操作   
		    }   
		    for ( var i = 0; i < 17; i++) {   
		        sum += Wi[i] * a_idCard[i];            // 加权求和   
		    }   
		    valCodePosition = sum % 11;                // 得到验证码所位置   
		    if (a_idCard[17] == ValideCode[valCodePosition]) {   
		        return true;   
		    } else {   
		        return false;   
		    }
	},"身份证不合法");

	jQuery.validator.addMethod("stringCheck",function(value,element,params){
	    var length = value.length;  
        var reg =/[^\a-zA-Z\u4E00-\u9FA5]/g;
      return this.optional(element) || (length<=20&&!reg.test(value));   
	},"公司名称非法");

	jQuery.validator.addMethod("telCheck",function(value,element,params){
		return this.optional(element) || (/^([0-9]{1,9}(\-)?)?([0-9]{1,9}){1}(\-[0-9]{1,9})?$/.test(value));  
	},"电话号码非法");

	jQuery.validator.addMethod("emailCheck",function(value,element,params){
		return this.optional(element) || (/^(\w)+(\d+|\w+)*@(\w)+((\.\w+)+)$/.test(value));
	},"非法的email地址");

	jQuery.validator.addMethod("mobileCheck",function(value,element,params){
		return this.optional(element) || (/^1[3|4|5|7|8|9][0-9]\d{8}$/.test(value));
	},"手机号非法");
});
