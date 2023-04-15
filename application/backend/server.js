const express=require("express")
const app=express()
const multer = require('multer');
require('dotenv').config() 
const bodyParser = require('body-parser');
const uuid=require("uuid")
const fs=require("fs") 
const { spawn } = require('child_process');

app.use(bodyParser.json());
app.use(bodyParser.urlencoded({ extended: true }));


const upload = multer({ dest: './uploads/' }); // Set the destination folder for uploaded files

app.route("/find-currency")
  .get((req,res)=>{
    console.log("from get root");
  })
  .post(upload.single('image'),async (req,res)=>{  
    try{
      let file=req.file; 
      let default_extension=".jpg"
      fs.rename(file.destination+file.filename,file.destination+file.filename+default_extension ,async function(err) {
        if (err) {
          res.status(200).json({error:true,result:"Server Error."})
        } else { 
          findResult(file.destination+file.filename+default_extension)
          .then(async (result)=>{ 
            res.status(200).json({success:true,result:result})
            let removed=false
            while(!removed){
              removed=await removeFile(file.destination+file.filename+default_extension) 
            }
          })
          .catch((error)=>{
            res.status(200).json({success:true,result:error}) 
          })
          // res.status(200).json({success:true,result:"result"})
        }
      });  
    }catch(error){ 
      res.status(200).json({error:true,result:"Server Error."})
    }
   // res.status(200).json({success:true,result:"The given currency is Fake"})
  })

//function to calculate the answer
async function findResult(path){ 
  return new Promise((res,rej)=>{
    try{
      const req=JSON.stringify({path,method:"imageProcess"})
      const script = spawn('python', ["image.py",req]);  
      // Read the output from the Python script
      script.stdout.on('data', function(data) {
        let result = data.toString().trim();   
        res(result)
      });
    
      // Handle errors
      script.stderr.on('data', function(data) {
        let error=data.toString() 
        res(error)
      });
      
      script.on('exit', function(code) {
        
      });
    }catch(error){
      res({error:true,result:"Server Error."})
    }
  }) 
}


//function to remove the particular file
async function removeFile(imagePath){ 
  try{
    fs.rmSync(imagePath)
  }catch(error){ 
    return false
  }
  return true
}

app.listen(process.env.PORT,()=>{
  console.log(`Running in ${process.env.PORT}`);
})