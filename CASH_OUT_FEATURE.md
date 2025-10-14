# Cash Out Feature Implementation

## Overview
A comprehensive cash out feature has been added to the Purchase Management module. This feature allows users to distribute cash out amounts across pending purchases in chronological order, with automatic calculation and manual editing capabilities.

## Features Implemented

### 1. **Data Models**
- **CashOut**: Main data model for cash out transactions
  - Stores total amount, allocations, notes, and timestamps
  - Located in: `data/CashOut.kt`

- **PurchaseAllocation**: Represents allocation details per purchase
  - Tracks allocated amount, previous/new payment status
  - Stores customer and purchase information

### 2. **Repository Layer** (`repositories/CashOutRepository.kt`)
- **Firebase Integration**: Uses Firestore transactions for atomicity
- **processCashOut()**: 
  - Atomically updates multiple purchases and customer balances
  - Uses Firebase transactions to ensure data consistency
  - Updates purchase payment status (PENDING → PARTIALLY_PAID → PAID)
  - Increases customer balance by allocated amounts

- **getPendingPurchases()**: 
  - Fetches all pending/partially paid purchases
  - Sorts by creation time (chronological order)
  - Filters out reversed transactions

- **listenToCashOutHistory()**: 
  - Real-time listener for cash out transaction history
  - Automatically updates UI when new cash outs are added

### 3. **ViewModel Layer** (`viewmodels/PurchaseViewModel.kt`)
- **calculateCashOutAllocations()**: 
  - Automatically distributes cash out amount across pending purchases
  - Follows chronological order (first purchase gets paid first)
  - Calculates new payment status for each purchase

- **processCashOut()**: 
  - Validates cash out amount and allocations
  - Processes the transaction through repository
  - Manages loading states and error handling

- **State Management**: 
  - Added `cashOuts` list to track history
  - Added `isCashingOut` flag for loading state

### 4. **UI Components**

#### Cash Out Dialog (`ui/components/CashOutDialog.kt`)
- **Amount Input**: User enters cash out amount
- **Auto Calculate**: Button to automatically distribute amount
- **Editable Allocations**: Users can manually adjust allocation amounts
- **Visual Summary**: 
  - Shows firm name, date, pending amount, and allocated amount
  - Real-time total calculation with validation
  - Error highlighting if total exceeds cash out amount
- **Notes Field**: Optional notes for the transaction
- **Save Transaction**: Only saves when user explicitly clicks "Save Cash Out"

#### Cash Out History Dialog (`ui/components/CashOutHistoryDialog.kt`)
- **Transaction List**: Shows all cash out transactions in reverse chronological order
- **Expandable Details**: Click to view allocation breakdown
- **Transaction Info**: Date, total amount, notes, and affected purchases
- **Allocation Details**: Shows which purchases were paid and their new status
- **Status Indicators**: Visual badges for PAID, PARTIALLY_PAID, and PENDING

#### Purchase Screen Integration (`ui/PurchaseScreen.kt`)
- **Cash Out Button**: Green button in the header
- **Cash Out History Button**: Outlined button to view transaction history
- **Icon Integration**: Uses Material Icons for better UX

### 5. **Business Logic**

#### Distribution Algorithm
1. Fetch all pending/partially paid purchases (sorted chronologically)
2. For each purchase in order:
   - Calculate remaining amount to pay (grandTotal - amountPaid)
   - Allocate minimum of (remaining cash out, remaining purchase amount)
   - Update payment status based on new amount paid
   - Subtract allocated amount from cash out balance
3. Continue until cash out amount is exhausted or all purchases are paid

#### Example Scenario (from user requirements)
```
Purchases:
- Purchase 1: ₹200 pending
- Purchase 2: ₹300 pending  
- Purchase 3: ₹100 pending
- Purchase 4: ₹200 pending

Cash Out: ₹550

Result:
- Purchase 1: Gets ₹200 (PAID) → Remaining ₹350
- Purchase 2: Gets ₹300 (PAID) → Remaining ₹50
- Purchase 3: Gets ₹50 (PARTIALLY_PAID) → Remaining ₹0
- Purchase 4: Gets ₹0 (PENDING) → Remaining ₹0
```

#### Customer Balance Updates
- For each customer with allocated purchases:
  - Balance increases by total allocated amount
  - This represents money received from customer
  - Updates atomically with purchase updates

### 6. **Firebase Integration**
- **Firestore Transactions**: Ensures atomicity across:
  - Multiple purchase updates
  - Multiple customer balance updates  
  - Cash out record creation
- **Async Operations**: All database operations use `withContext(Dispatchers.IO)`
- **Real-time Listeners**: Automatic UI updates for cash out history
- **Error Handling**: Comprehensive try-catch with user-friendly messages

### 7. **Database Schema**

#### Cash Out Collection
```
cash_out/
  {cashOutId}/
    userId: String
    totalAmount: Double
    notes: String
    createdAt: Timestamp
    purchaseAllocations: [
      {
        purchaseId: String
        firmName: String
        purchaseDate: String
        customerId: String
        allocatedAmount: Double
        previousAmountPaid: Double
        newAmountPaid: Double
        previousPaymentStatus: String
        newPaymentStatus: String
      }
    ]
```

## Files Created/Modified

### New Files
1. `data/CashOut.kt` - Cash out data models
2. `repositories/CashOutRepository.kt` - Cash out business logic
3. `ui/components/CashOutDialog.kt` - Cash out input UI
4. `ui/components/CashOutHistoryDialog.kt` - Transaction history UI

### Modified Files
1. `viewmodels/PurchaseViewModel.kt` - Added cash out methods and state
2. `ui/PurchaseScreen.kt` - Added cash out buttons and dialogs
3. `main.kt` - Initialize CashOutRepository

## Usage Flow

1. **Initiate Cash Out**:
   - Click "Cash Out" button in Purchase Management
   - Enter cash out amount
   - Click "Calculate" to auto-distribute

2. **Review & Edit**:
   - Review automatic allocation
   - Edit individual amounts if needed
   - Add optional notes

3. **Save Transaction**:
   - Click "Save Cash Out" to process
   - System atomically updates:
     - Purchase payment amounts
     - Purchase payment statuses
     - Customer balances
     - Cash out history

4. **View History**:
   - Click "Cash Out History" button
   - View all past transactions
   - Expand for allocation details

## Technical Highlights

- ✅ **Atomic Transactions**: Firebase transactions ensure data consistency
- ✅ **Chronological Distribution**: First-in-first-out allocation
- ✅ **Manual Override**: Users can edit auto-calculated amounts
- ✅ **Real-time Updates**: Instant UI refresh on data changes
- ✅ **Comprehensive Validation**: Amount checks and error messages
- ✅ **Professional UI**: Modern design with visual feedback
- ✅ **Transaction History**: Complete audit trail with expandable details

## Future Enhancements (Optional)

1. **Print/Export**: Generate PDF receipts for cash out transactions
2. **Bulk Operations**: Cash out for specific customers or date ranges
3. **Partial Allocation**: Option to partially allocate to selected purchases
4. **Scheduled Cash Out**: Automated cash out on specific dates
5. **Analytics**: Cash flow reports and visualizations

