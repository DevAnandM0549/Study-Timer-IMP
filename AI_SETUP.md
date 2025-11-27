# AI Chat Setup Instructions

## 🔐 Secure API Key Configuration

### For Local Development:

1. **Never commit your API key to Git!**

2. **Option A: Environment Variable (Recommended)**
   ```bash
   # Windows (PowerShell)
   $env:PERPLEXITY_API_KEY="your-actual-api-key-here"
   
   # Windows (CMD)
   set PERPLEXITY_API_KEY=your-actual-api-key-here
   
   # Mac/Linux
   export PERPLEXITY_API_KEY="your-actual-api-key-here"
   ```

3. **Option B: IDE Configuration**
   - In IntelliJ IDEA: Run → Edit Configurations → Environment Variables
   - Add: `PERPLEXITY_API_KEY=your-actual-api-key-here`

### For Railway Deployment:

1. Go to your Railway project dashboard
2. Click on your service
3. Go to "Variables" tab
4. Click "New Variable"
5. Add:
   - **Variable Name:** `PERPLEXITY_API_KEY`
   - **Value:** Your actual Perplexity API key
6. Click "Add" and Railway will automatically redeploy

### Testing the API Key:

After setting up, the app will use the environment variable automatically. The key is never stored in code or committed to Git.

### Getting a Perplexity API Key:

1. Go to https://www.perplexity.ai/
2. Sign up for an account
3. Navigate to API settings
4. Generate a new API key
5. Copy the key and use it in the environment variable

## 🚀 Features

The AI Assistant can help with:
- Study tips and techniques
- Time management advice
- Productivity strategies
- Motivation and focus tips
- General study-related questions

## 🔒 Security Notes

- ✅ API key is stored in environment variables only
- ✅ Never committed to Git
- ✅ Separate for local dev and production
- ✅ Can be rotated anytime without code changes
